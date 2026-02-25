document.addEventListener('DOMContentLoaded', () => {
    const statusBadge = document.getElementById('status-badge');
    const scheduledTimeSpan = document.getElementById('scheduled-time');
    const serverTimezoneSpan = document.getElementById('server-timezone');
    const serverTimeTop = document.getElementById('server-time-top');
    const serverTimeBottom = document.getElementById('server-time-bottom');
    const tzLabels = document.querySelectorAll('.tz-label');

    const datetimeInput = document.getElementById('schedule-datetime');
    const sleepInput = document.getElementById('sleep');
    const randomSleepInput = document.getElementById('random-sleep');

    const refreshBtn = document.getElementById('refresh-btn');
    const cancelBtn = document.getElementById('cancel-btn');
    const setScheduleBtn = document.getElementById('set-schedule-btn');

    const historySection = document.getElementById('history-section');
    const historyBody = document.getElementById('history-body');

    const updateStatus = async () => {
        try {
            const response = await fetch('/small/api/status');
            const data = await response.json();

            statusBadge.textContent = data.status;
            statusBadge.className = 'badge ' + getBadgeClass(data.status);

            scheduledTimeSpan.textContent = data.scheduledTime || 'None';
            serverTimezoneSpan.textContent = data.timezone;

            updateServerTimeDisplay(data.currentTime, data.timezone);
            renderHistory(data.history);
        } catch (error) {
            console.error('Error fetching status:', error);
            statusBadge.textContent = 'ERROR';
        }
    };

    const updateServerTimeDisplay = (timeStr, timezone) => {
        if (!timeStr) return;
        const formatted = timeStr.replace('T', ' ').substring(0, 19);
        if (serverTimeTop) serverTimeTop.textContent = formatted;
        if (serverTimeBottom) serverTimeBottom.textContent = formatted;
        tzLabels.forEach(el => el.textContent = timezone);
    };

    const renderHistory = (history) => {
        if (!history || history.length === 0) {
            historySection.style.display = 'none';
            return;
        }

        historySection.style.display = 'block';
        historyBody.innerHTML = '';

        history.forEach(item => {
            const row = document.createElement('tr');

            const timeCell = document.createElement('td');
            timeCell.textContent = item.scheduledTime.replace('T', ' ');

            const statusCell = document.createElement('td');
            const statusClass = 'status-' + item.status.toLowerCase();
            statusCell.innerHTML = `<span class="history-status ${statusClass}">${item.status}</span>`;

            const errorCell = document.createElement('td');
            errorCell.className = 'history-error';
            errorCell.textContent = item.error || '-';
            errorCell.title = item.error || '';

            row.appendChild(timeCell);
            row.appendChild(statusCell);
            row.appendChild(errorCell);
            historyBody.appendChild(row);
        });
    };

    const getBadgeClass = (status) => {
        if (status.includes('Scheduled')) return 'badge-scheduled';
        if (status === 'Working') return 'badge-working';
        if (status === 'Idle') return 'badge-idle';
        return 'badge-none';
    };

    const setSchedule = async () => {
        const time = datetimeInput.value;
        const sleep = parseInt(sleepInput.value);
        const randomSleep = parseInt(randomSleepInput.value);

        if (!time) {
            alert('Please select a date and time');
            return;
        }

        try {
            const response = await fetch('/small/api/set', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ time, sleep, randomSleep })
            });
            const data = await response.json();
            alert(`Schedule set! Server Time: ${data.serverTime} (${data.timezone})`);
            updateStatus();
        } catch (error) {
            console.error('Error setting schedule:', error);
            alert('Failed to set schedule');
        }
    };

    const cancelSchedule = async () => {
        try {
            const response = await fetch('/small/api/cancel', { method: 'POST' });
            const data = await response.json();
            alert(data.message);
            updateStatus();
        } catch (error) {
            console.error('Error canceling schedule:', error);
            alert('Failed to cancel schedule');
        }
    };

    // Set default datetime to 1 minute from now
    const now = new Date();
    now.setMinutes(now.getMinutes() + 1);
    now.setSeconds(0);
    now.setMilliseconds(0);
    const tzOffset = now.getTimezoneOffset() * 60000;
    const localISOTime = new Date(now - tzOffset).toISOString().slice(0, 16);
    datetimeInput.value = localISOTime;

    refreshBtn.addEventListener('click', updateStatus);
    setScheduleBtn.addEventListener('click', setSchedule);
    cancelBtn.addEventListener('click', cancelSchedule);

    // Initial status check
    updateStatus();

    // Time-only refresh (every 5 seconds) - always runs
    setInterval(async () => {
        try {
            const response = await fetch('/small/api/status');
            const data = await response.json();
            updateServerTimeDisplay(data.currentTime, data.timezone);
        } catch (err) {
            console.warn("Fast clock refresh failed", err);
        }
    }, 5000);

    // Auto-refresh logic (every 7 seconds) - only runs if checked
    const autoRefreshCheckbox = document.getElementById('auto-refresh');
    setInterval(() => {
        if (autoRefreshCheckbox.checked) {
            updateStatus();
        }
    }, 7000);
});
