const express = require('express');
const http = require('http');
const WebSocket = require('ws');
const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');
const cors = require('cors');

const app = express();
const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

const PROJECT_ROOT = path.resolve(__dirname, '../../');
const GRADLEW = process.platform === 'win32' ? 'gradlew.bat' : './gradlew';

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, '../public')));

// Store active process to allow cancellation (optional)
let activeProcess = null;

// Helper to broadcast logs
const broadcast = (data) => {
    wss.clients.forEach(client => {
        if (client.readyState === WebSocket.OPEN) {
            client.send(JSON.stringify(data));
        }
    });
};

// --- API Endpoints ---

// List available specs in examples/ directory
app.get('/api/specs', (req, res) => {
    const examplesDir = path.join(PROJECT_ROOT, 'examples');
    fs.readdir(examplesDir, (err, files) => {
        if (err) {
            return res.status(500).json({ error: 'Failed to read examples directory' });
        }
        const specs = files.filter(f => f.endsWith('.yaml') || f.endsWith('.json'));
        res.json(specs);
    });
});

// Generate Tests
app.post('/api/generate', (req, res) => {
    const { specFile, outputDir, basePackage, baseUrl, mode } = req.body;

    if (activeProcess) {
        return res.status(409).json({ error: 'A process is already running' });
    }

    const modeArg = mode || 'new-project';
    const outArg = outputDir || 'generated-tests';
    const pkgArg = basePackage || 'com.generated.api';
    const urlArg = baseUrl || 'http://localhost:8080';

    // Construct command string for shell execution to handle quotes correctly
    // Note: We use double quotes for the --args value to ensure Gradle treats it as a single argument
    const gradleArgs = `--spec "examples/${specFile}" --output "${outArg}" --mode "${modeArg}" --basePackage "${pkgArg}" --baseUrl "${urlArg}"`;
    // Escape double quotes inside the args if necessary (simplistic approach here)
    const command = `${GRADLEW} :cli:run --args='${gradleArgs}'`;

    console.log(`Starting generation with command: ${command}`);
    broadcast({ type: 'status', message: 'Starting generation process...' });

    try {
        // execute the command string directly with shell: true
        activeProcess = spawn(command, { cwd: PROJECT_ROOT, shell: true });

        activeProcess.stdout.on('data', (data) => {
            broadcast({ type: 'log', message: data.toString() });
        });

        activeProcess.stderr.on('data', (data) => {
            broadcast({ type: 'error', message: data.toString() });
        });

        activeProcess.on('close', (code) => {
            console.log(`Generation process exited with code ${code}`);
            broadcast({ type: 'status', message: code === 0 ? 'Generation Complete!' : 'Generation Failed', code });
            activeProcess = null;
        });

        res.json({ status: 'started' });
    } catch (e) {
        activeProcess = null;
        res.status(500).json({ error: e.message });
    }
});

// Run Generated Tests
app.post('/api/run-tests', (req, res) => {
    if (activeProcess) {
        return res.status(409).json({ error: 'A process is already running' });
    }

    const { outputDir } = req.body;
    const targetDir = outputDir || 'generated-tests';

    console.log(`Running tests in ${targetDir}`);
    broadcast({ type: 'status', message: 'Running generated tests via Gradle...' });

    // ./gradlew -p generated-tests test
    const args = ['-p', targetDir, 'test'];

    try {
        activeProcess = spawn(GRADLEW, args, { cwd: PROJECT_ROOT, shell: true });

        activeProcess.stdout.on('data', (data) => {
            broadcast({ type: 'log', message: data.toString() });
        });

        activeProcess.stderr.on('data', (data) => {
            broadcast({ type: 'error', message: data.toString() });
        });

        activeProcess.on('close', (code) => {
            console.log(`Test run exited with code ${code}`);
            broadcast({ type: 'status', message: code === 0 ? 'Tests Passed!' : 'Tests Failed', code });
            activeProcess = null;
        });

        res.json({ status: 'started' });
    } catch (e) {
        activeProcess = null;
        res.status(500).json({ error: e.message });
    }
});

// Open/Serve JUnit Report
// The report is generated at generated-tests/build/reports/tests/test/index.html
// We can serve this static folder or open it with 'open' command
app.get('/api/report/url', (req, res) => {
    // We'll serve the report directory statically under /report
    // Dynamically check if report exists
    const reportDir = path.join(PROJECT_ROOT, 'generated-tests/build/reports/tests/test');
    if (fs.existsSync(reportDir)) {
        res.json({ available: true, url: '/report/index.html' });
    } else {
        res.json({ available: false });
    }
});

// List Generated Files
app.get('/api/files', (req, res) => {
    const genDir = path.join(PROJECT_ROOT, 'generated-tests/src/test/java');

    // Recursive file walker
    const walk = (dir, rootDir) => {
        let results = [];
        if (!fs.existsSync(dir)) return results;

        const list = fs.readdirSync(dir);
        list.forEach((file) => {
            file = path.resolve(dir, file);
            const stat = fs.statSync(file);
            if (stat && stat.isDirectory()) {
                results = results.concat(walk(file, rootDir));
            } else {
                results.push({
                    name: path.basename(file),
                    path: path.relative(rootDir, file),
                    size: stat.size
                });
            }
        });
        return results;
    };

    try {
        // We list files starting from the generated-tests root/src/test/java to show the user the code
        // Or maybe just the root of generated-tests
        const root = path.join(PROJECT_ROOT, 'generated-tests');
        const files = walk(root, root);
        res.json(files);
    } catch (e) {
        res.status(500).json({ error: e.message });
    }
});

// Serve the report directory at /report
app.use('/report', express.static(path.join(PROJECT_ROOT, 'generated-tests/build/reports/tests/test')));

const PORT = 3000;
server.listen(PORT, () => {
    console.log(`Server running on http://localhost:${PORT}`);
    console.log(`Project Root: ${PROJECT_ROOT}`);
});
