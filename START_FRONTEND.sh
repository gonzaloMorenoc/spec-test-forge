#!/bin/bash
echo "🚀 Iniciando SpecForge Frontend..."

# Check Node
if ! command -v node &> /dev/null
then
    echo "❌ Node.js no encontrado. Por favor instálalo para continuar."
    exit 1
fi

# Go to server directory
cd frontend/server

# Kill any existing process on port 3000
if lsof -Pi :3000 -sTCP:LISTEN -t >/dev/null ; then
    echo "🔍 Limpiando proceso anterior en puerto 3000..."
    kill -9 $(lsof -t -i:3000)
fi

# Install dependencies if not present
if [ ! -d "node_modules" ]; then
    echo "📦 Instalando dependencias..."
    npm install
fi

# Start server
echo "✅ Servidor listo en http://localhost:3000"
echo "🌐 Abriendo navegador..."
if [[ "$OSTYPE" == "darwin"* ]]; then
    open http://localhost:3000
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    xdg-open http://localhost:3000
fi

node index.js
