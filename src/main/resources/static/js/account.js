(() => {
    'use strict';
    document.querySelectorAll('[data-password-toggle]').forEach((button) => {
        const input = document.getElementById(button.dataset.passwordToggle);
        if (!input) return;
        button.hidden = false;
        const label = button.getAttribute('aria-label');
        button.addEventListener('click', () => {
            const reveal = input.type === 'password';
            input.type = reveal ? 'text' : 'password';
            button.textContent = reveal ? 'Hide' : 'Show';
            button.setAttribute('aria-pressed', String(reveal));
            button.setAttribute('aria-label', reveal ? label.replace('Show', 'Hide') : label);
        });
    });
    document.querySelector('[data-form-errors]')?.focus({ preventScroll: true });
})();
