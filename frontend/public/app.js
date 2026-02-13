document.addEventListener('DOMContentLoaded', () => {
    // UI Elements
    const terminal = document.getElementById('terminal-output');
    const specSelect = document.getElementById('spec-select');
    const generateForm = document.getElementById('generate-form');
    const btnGenerate = document.getElementById('btn-generate');
    const btnRunTests = document.getElementById('btn-run-tests');
    const btnViewReport = document.getElementById('btn-view-report');
    const ollamaStatus = document.getElementById('ollama-status');

    // Navigation Elements
    const navItems = document.querySelectorAll('.nav-item');
    const views = {
        'dashboard': [document.querySelector('.config-card'), document.querySelector('.actions-card')],
        'generated-files': [document.getElementById('view-generated-files')],
        'settings': [document.getElementById('view-settings')]
    };

    // State
    let isGenerating = false;
    let isRunningTests = false;

    // --- Navigation Logic ---
    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const targetView = item.dataset.view;

            // Update Active Nav
            navItems.forEach(n => n.classList.remove('active'));
            item.classList.add('active');

            // Update Header Title
            const pageTitle = document.querySelector('.top-bar h1');
            pageTitle.textContent = item.textContent.trim().replace(/[^\w\s]/g, '');

            // Toggle Views
            // First hide all view content
            Object.values(views).flat().forEach(el => {
                if (el) el.classList.add('hidden');
            });

            // Show target view elements
            if (views[targetView]) {
                views[targetView].forEach(el => {
                    if (el) el.classList.remove('hidden');
                });
            }

            // Load specific data
            if (targetView === 'generated-files') {
                loadFiles();
            }
        });
    });

    // File List Logic
    document.getElementById('btn-refresh-files').addEventListener('click', loadFiles);

    function loadFiles() {
        const container = document.getElementById('file-list-container');
        container.innerHTML = '<div class="empty-state">Loading files...</div>';

        fetch('/api/files')
            .then(res => res.json())
            .then(files => {
                if (files.length === 0) {
                    container.innerHTML = '<div class="empty-state">No generated files found.</div>';
                    return;
                }

                container.innerHTML = '';
                files.forEach(file => {
                    const div = document.createElement('div');
                    div.className = 'file-item';
                    div.innerHTML = `
                        <span class="file-name">${file.path}</span>
                        <span class="file-size">${(file.size / 1024).toFixed(1)} KB</span>
                    `;
                    container.appendChild(div);
                });
            })
            .catch(err => {
                container.innerHTML = `<div class="empty-state error">Error loading files: ${err.message}</div>`;
            });
    }

    // --- WebSocket Connection ---
    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const ws = new WebSocket(`${wsProtocol}//${window.location.host}`);

    ws.onopen = () => {
        appendLog('Connected to SpecForge Backend', 'system');
    };

    ws.onmessage = (event) => {
        const data = JSON.parse(event.data);
        if (data.type === 'log') {
            appendLog(data.message);
        } else if (data.type === 'error') {
            appendLog(data.message, 'error');
        } else if (data.type === 'status') {
            appendLog(`[STATUS] ${data.message}`, 'system');

            // Auto-enable buttons based on status messages if needed
            if (data.message.includes('Generation Complete')) {
                isGenerating = false;
                resetButtons();
                btnRunTests.disabled = false;
                showToast('Tests Generated Successfully', 'success');
            } else if (data.message.includes('Generation Failed')) {
                isGenerating = false;
                resetButtons();
                showToast('Generation Failed', 'error');
            } else if (data.message.includes('Tests Passed') || data.message.includes('Tests Failed')) {
                isRunningTests = false;
                resetButtons();
                checkReportAvailability();
                showToast(data.message.includes('Passed') ? 'Tests Passed' : 'Tests Failed', data.message.includes('Passed') ? 'success' : 'warning');
            }
        }
    };

    ws.onclose = () => {
        appendLog('Disconnected from backend', 'error');
    };

    // --- Actions ---

    // 1. Load Specs
    fetch('/api/specs')
        .then(res => res.json())
        .then(files => {
            specSelect.innerHTML = '<option value="" disabled selected>Select a spec file...</option>';
            files.forEach(file => {
                const opt = document.createElement('option');
                opt.value = file;
                opt.textContent = file;
                specSelect.appendChild(opt);
            });
        })
        .catch(err => {
            appendLog('Failed to load specs: ' + err.message, 'error');
        });

    // 2. Generate Tests
    generateForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (isGenerating || isRunningTests) return;

        const formData = new FormData(generateForm);
        const payload = Object.fromEntries(formData.entries());

        setLoading(true, 'Generating...');
        appendLog('--- Starting Generation ---', 'system');

        try {
            const res = await fetch('/api/generate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const data = await res.json();
            if (data.error) throw new Error(data.error);
        } catch (err) {
            appendLog(err.message, 'error');
            setLoading(false);
        }
    });

    // 3. Run Tests
    btnRunTests.addEventListener('click', async () => {
        if (isGenerating || isRunningTests) return;

        setLoading(true, 'Running Tests...');
        appendLog('--- Starting Tests ---', 'system');

        try {
            const res = await fetch('/api/run-tests', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ outputDir: document.getElementById('output-dir').value })
            });
            const data = await res.json();
            if (data.error) throw new Error(data.error);
        } catch (err) {
            appendLog(err.message, 'error');
            setLoading(false);
        }
    });

    // 4. Check Report
    function checkReportAvailability() {
        fetch('/api/report/url')
            .then(res => res.json())
            .then(data => {
                btnViewReport.disabled = !data.available;
            });
    }

    // Initial check
    checkReportAvailability();

    // Check Ollama (Mock check for now or try to fetch localhost if mixed content allowed)
    // Client-side check to localhost:11434 will likely fail due to CORS unless Ollama is configured
    // We'll just assume warning state until backend confirms (if we added backend check)
    // For now we leave it as warning yellow.

    // --- Helpers ---

    function appendLog(msg, type = 'normal') {
        const div = document.createElement('div');
        div.className = `log-line ${type}`;
        div.textContent = msg.trim();
        if (msg.trim()) {
            terminal.appendChild(div);
            terminal.scrollTop = terminal.scrollHeight;
        }
    }

    function setLoading(loading, text) {
        if (loading) {
            isGenerating = true; // or isRunningTests
            btnGenerate.disabled = true;
            btnRunTests.disabled = true;
            btnGenerate.classList.add('loading');
            document.body.style.cursor = 'wait';
        } else {
            resetButtons();
        }
    }

    function resetButtons() {
        isGenerating = false;
        isRunningTests = false;
        btnGenerate.disabled = false;
        btnRunTests.disabled = false; // Enabled after generating
        btnGenerate.classList.remove('loading');
        document.body.style.cursor = 'default';
    }

    // Clear Logs
    document.getElementById('btn-clear-logs').addEventListener('click', () => {
        terminal.innerHTML = '<div class="log-line system">Logs cleared.</div>';
    });

    // Toast Notification (Simple Implementation)
    function showToast(message, type = 'success') {
        const toast = document.createElement('div');
        toast.style.position = 'fixed';
        toast.style.top = '20px';
        toast.style.right = '20px';
        toast.style.padding = '1rem 1.5rem';
        toast.style.backgroundColor = type === 'success' ? '#10b981' : '#f59e0b';
        if (type === 'error') toast.style.backgroundColor = '#ef4444';
        toast.style.color = '#fff';
        toast.style.borderRadius = '8px';
        toast.style.boxShadow = '0 4px 6px rgba(0,0,0,0.1)';
        toast.style.zIndex = '1000';
        toast.style.animation = 'fadeIn 0.3s forwards';
        toast.textContent = message;

        document.body.appendChild(toast);
        setTimeout(() => {
            toast.style.animation = 'fadeOut 0.3s forwards';
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    }
});
