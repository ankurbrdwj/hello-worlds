let chart;
let candlestickSeries;
let autoRefreshInterval;

// Initialize chart on page load
document.addEventListener('DOMContentLoaded', () => {
    initializeChart();
    loadInstruments();
    setupEventListeners();
});

function initializeChart() {
    const chartContainer = document.getElementById('chart');

    // Check if LightweightCharts is loaded
    if (typeof LightweightCharts === 'undefined') {
        console.error('LightweightCharts library not loaded!');
        updateStatus('Error: Chart library failed to load', 'error');
        return;
    }

    console.log('LightweightCharts object:', LightweightCharts);
    console.log('Available methods:', Object.keys(LightweightCharts));

    chart = LightweightCharts.createChart(chartContainer, {
        width: chartContainer.clientWidth,
        height: 600,
        layout: {
            background: { color: '#1a1a1a' },
            textColor: '#d1d4dc',
        },
        grid: {
            vertLines: { color: '#2a2a2a' },
            horzLines: { color: '#2a2a2a' },
        },
        crosshair: {
            mode: LightweightCharts.CrosshairMode.Normal,
        },
        rightPriceScale: {
            borderColor: '#3a3a3a',
        },
        timeScale: {
            borderColor: '#3a3a3a',
            timeVisible: true,
            secondsVisible: false,
        },
    });

    console.log('Chart object:', chart);
    console.log('Chart methods:', Object.getOwnPropertyNames(Object.getPrototypeOf(chart)));
    console.log('Has addCandlestickSeries?', typeof chart.addCandlestickSeries);

    candlestickSeries = chart.addCandlestickSeries({
        upColor: '#26a69a',
        downColor: '#ef5350',
        borderVisible: false,
        wickUpColor: '#26a69a',
        wickDownColor: '#ef5350',
    });

    // Handle window resize
    window.addEventListener('resize', () => {
        chart.applyOptions({ width: chartContainer.clientWidth });
    });
}

async function loadInstruments() {
    try {
        console.log('Fetching instruments...');
        const response = await fetch('/instruments?minutes=1440');
        console.log('Response status:', response.status);

        if (!response.ok) throw new Error('Failed to fetch instruments: ' + response.status);

        const instruments = await response.json();
        console.log('Loaded instruments:', instruments);

        const select = document.getElementById('isin');

        // Clear existing options except the first one
        select.innerHTML = '<option value="">Select instrument...</option>';

        instruments.forEach(instrument => {
            const option = document.createElement('option');
            option.value = instrument.isin;
            option.textContent = `${instrument.isin} - ${instrument.description || 'No description'}`;
            select.appendChild(option);
        });

        updateStatus(`Loaded ${instruments.length} instruments`, 'success');
    } catch (error) {
        console.error('Error loading instruments:', error);
        updateStatus('Error loading instruments: ' + error.message, 'error');
    }
}

async function loadCandlesticks() {
    const isin = document.getElementById('isin').value;
    const minutes = document.getElementById('minutes').value;

    if (!isin) {
        updateStatus('Please select an ISIN', 'error');
        return;
    }

    const loadBtn = document.getElementById('loadBtn');
    const refreshBtn = document.getElementById('refreshBtn');
    loadBtn.disabled = true;
    refreshBtn.disabled = true;

    try {
        updateStatus('Loading candlesticks...', '');

        const response = await fetch(`/candlesticks?isin=${encodeURIComponent(isin)}&minutes=${minutes}`);
        if (!response.ok) throw new Error('Failed to fetch candlesticks');

        const candlesticks = await response.json();

        if (candlesticks.length === 0) {
            updateStatus('No candlestick data available for this instrument', 'error');
            candlestickSeries.setData([]);
            return;
        }

        // Transform data to Lightweight Charts format
        const chartData = candlesticks.map(candle => ({
            time: convertToTimestamp(candle.openTimestamp),
            open: candle.openPrice,
            high: candle.highPrice,
            low: candle.lowPrice,
            close: candle.closePrice
        }));

        // Sort by time
        chartData.sort((a, b) => a.time - b.time);

        candlestickSeries.setData(chartData);

        // Fit the chart to the data
        chart.timeScale().fitContent();

        updateStatus(`Loaded ${candlesticks.length} candlesticks for ${isin}`, 'success');

    } catch (error) {
        console.error('Error loading candlesticks:', error);
        updateStatus('Error loading candlesticks: ' + error.message, 'error');
    } finally {
        loadBtn.disabled = false;
        refreshBtn.disabled = false;
    }
}

function convertToTimestamp(dateString) {
    // Convert ISO string to Unix timestamp in seconds
    // Expected format: "2024-12-25T17:30:00"
    const date = new Date(dateString);
    return Math.floor(date.getTime() / 1000);
}

function setupEventListeners() {
    document.getElementById('loadBtn').addEventListener('click', loadCandlesticks);
    document.getElementById('refreshBtn').addEventListener('click', loadCandlesticks);

    document.getElementById('autoRefresh').addEventListener('change', (e) => {
        if (e.target.checked) {
            startAutoRefresh();
        } else {
            stopAutoRefresh();
        }
    });

    // Allow Enter key to load chart
    document.getElementById('isin').addEventListener('change', () => {
        if (document.getElementById('isin').value) {
            loadCandlesticks();
        }
    });
}

function startAutoRefresh() {
    stopAutoRefresh(); // Clear any existing interval
    autoRefreshInterval = setInterval(() => {
        if (document.getElementById('isin').value) {
            loadCandlesticks();
        }
    }, 5000); // Refresh every 5 seconds
    updateStatus('Auto-refresh enabled (every 5 seconds)', 'success');
}

function stopAutoRefresh() {
    if (autoRefreshInterval) {
        clearInterval(autoRefreshInterval);
        autoRefreshInterval = null;
    }
}

function updateStatus(message, type) {
    const statusEl = document.getElementById('status');
    statusEl.textContent = message;
    statusEl.className = 'status' + (type ? ' ' + type : '');
}