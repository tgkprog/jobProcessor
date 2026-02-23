/**
 * Common Header Loader for Job Processor Engine
 * Injects the navigation bar into every page.
 */
document.addEventListener('DOMContentLoaded', () => {
    // Find the placeholder or insert at start of .container
    let navContainer = document.getElementById('common-nav');

    if (!navContainer) {
        const mainContainer = document.querySelector('.container');
        if (mainContainer) {
            navContainer = document.createElement('div');
            navContainer.id = 'common-nav';
            mainContainer.insertBefore(navContainer, mainContainer.firstChild);
        }
    }

    if (navContainer) {
        const currentPath = window.location.pathname;
        const page = currentPath.split('/').pop() || 'index.html';

        navContainer.innerHTML = `
            <div class="nav">
                <a href="index.html" class="${page === 'index.html' ? 'active' : ''}">Home</a>
                <a href="jobs.html" class="${page === 'jobs.html' ? 'active' : ''}">Jobs Tracking</a>
                <a href="jobsProcs.html" class="${page === 'jobsProcs.html' ? 'active' : ''}">Manage Processors</a>
                <a href="admin.html" class="${page === 'admin.html' ? 'active' : ''}">Admin Control</a>
                <a href="dashboard.html" class="${page === 'dashboard.html' ? 'active' : ''}">Dashboard</a>
            </div>
        `;
    }
});
