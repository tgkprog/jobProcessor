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
        fetch('res/header.html')
            .then(response => response.text())
            .then(html => {
                navContainer.innerHTML = html;

                // Set active class based on current page
                const currentPath = window.location.pathname;
                const page = currentPath.split('/').pop() || 'index.html';

                const links = navContainer.querySelectorAll('.nav a');
                links.forEach(link => {
                    const href = link.getAttribute('href');
                    if (href === page || (page === '' && href === 'index.html')) {
                        link.classList.add('active');
                    }
                });
            })
            .catch(error => console.error('Error loading header:', error));
    }
});
