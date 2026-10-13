(() => {
    'use strict';

    const nav = document.querySelector('.apex-nav');
    const updateNav = () => nav?.classList.toggle('is-scrolled', window.scrollY > 24);
    updateNav();
    window.addEventListener('scroll', updateNav, { passive: true });

    document.querySelectorAll('[data-current-year]').forEach((node) => {
        node.textContent = String(new Date().getFullYear());
    });

    document.querySelectorAll('[data-auto-submit]').forEach((select) => {
        select.addEventListener('change', () => select.form?.requestSubmit());
    });

    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (!reduceMotion && window.gsap) {
        if (window.ScrollTrigger) {
            window.gsap.registerPlugin(window.ScrollTrigger);
        }

        const heroTimeline = window.gsap.timeline({ defaults: { ease: 'power3.out' } });
        heroTimeline
            .from('.hero__media img', { scale: 1.12, duration: 1.8 })
            .from('.hero-kicker', { y: 18, opacity: 0, duration: .7 }, '-=1.1')
            .from('.hero h1', { y: 70, opacity: 0, duration: 1.1 }, '-=.7')
            .from('.hero__lead, .hero__actions', { y: 25, opacity: 0, stagger: .12, duration: .75 }, '-=.6');

        window.gsap.utils.toArray('.reveal').forEach((element) => {
            window.gsap.from(element, {
                scrollTrigger: { trigger: element, start: 'top 88%', once: true },
                y: 36,
                opacity: 0,
                duration: .85,
                ease: 'power2.out'
            });
        });

        window.gsap.utils.toArray('.parallax-media img').forEach((image) => {
            window.gsap.to(image, {
                yPercent: -10,
                ease: 'none',
                scrollTrigger: { trigger: image.parentElement, start: 'top bottom', end: 'bottom top', scrub: true }
            });
        });

        document.querySelectorAll('[data-count]').forEach((node) => {
            const target = Number(node.dataset.count);
            const counter = { value: 0 };
            window.gsap.to(counter, {
                value: target,
                duration: 1.4,
                ease: 'power2.out',
                scrollTrigger: { trigger: node, start: 'top 90%', once: true },
                onUpdate: () => { node.textContent = String(Math.round(counter.value)); }
            });
        });
    }

    // Future Three.js integration point: replace the fallback <img> inside this stage
    // with a progressively enhanced canvas while retaining the accessible image fallback.
    document.querySelectorAll('[data-three-stage]').forEach((stage) => {
        stage.dataset.viewerState = 'static-fallback';
    });
})();
