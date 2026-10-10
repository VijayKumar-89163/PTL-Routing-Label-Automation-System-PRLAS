const ANALYTICS_REFRESH_INTERVAL_MS = 3000; // 3 seconds — "real-time" via polling

document.addEventListener("DOMContentLoaded", () => {
    fetchAnalyticsSummary();
    setInterval(fetchAnalyticsSummary, ANALYTICS_REFRESH_INTERVAL_MS);
});

async function fetchAnalyticsSummary() {
    try {
        const res = await fetch('/api/v1/analytics/summary');
        if (!res.ok) return;
        const data = await res.json();

        setPeriodCard('today', data.today);
        setPeriodCard('week', data.week);
        setPeriodCard('month', data.month);
        setPeriodCard('qtd', data.qtd);
        setPeriodCard('year', data.year);

        renderTrendChart(data.trend || []);

        const lastRefreshed = document.getElementById('lastRefreshedAt');
        if (lastRefreshed) lastRefreshed.innerText = new Date().toLocaleTimeString();

    } catch (e) {
        console.error('Analytics fetch failed:', e);
    }
}

function setPeriodCard(prefix, periodData) {
    if (!periodData) return;
    const totalEl = document.getElementById(`${prefix}Total`);
    const foundEl = document.getElementById(`${prefix}Found`);
    const notFoundEl = document.getElementById(`${prefix}NotFound`);

    if (totalEl) totalEl.innerText = periodData.total;
    if (foundEl) foundEl.innerText = `${periodData.found} Found`;
    if (notFoundEl) notFoundEl.innerText = `${periodData.notFound} Not Found`;
}

function renderTrendChart(trend) {
    const container = document.getElementById('trendChart');
    if (!container) return;

    if (!trend.length) {
        container.innerHTML = `<small class="text-muted">No data yet</small>`;
        return;
    }

    // Scale bar heights relative to the highest single-day count in the window
    const maxCount = Math.max(1, ...trend.map(d => Math.max(d.found, d.notFound)));
    const MAX_BAR_HEIGHT_PX = 150;

    container.innerHTML = trend.map(day => {
        const foundHeight = Math.max(2, Math.round((day.found / maxCount) * MAX_BAR_HEIGHT_PX));
        const notFoundHeight = Math.max(2, Math.round((day.notFound / maxCount) * MAX_BAR_HEIGHT_PX));
        const shortLabel = formatShortDate(day.date);

        return `
            <div class="trend-day" title="${day.date}: ${day.found} Found, ${day.notFound} Not Found">
                <div class="trend-bars">
                    <div class="trend-bar found" style="height:${foundHeight}px"></div>
                    <div class="trend-bar not-found" style="height:${notFoundHeight}px"></div>
                </div>
                <div class="trend-label">${shortLabel}</div>
            </div>
        `;
    }).join('');
}

function formatShortDate(isoDate) {
    const d = new Date(isoDate + 'T00:00:00');
    return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short' });
}
