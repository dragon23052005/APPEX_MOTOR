(() => {
    'use strict';

    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const gsap = window.gsap;
    const scrollTrigger = window.ScrollTrigger;
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

    if (!reduceMotion && gsap) {
        if (scrollTrigger) {
            gsap.registerPlugin(scrollTrigger);
        }

        const transition = document.querySelector('[data-page-transition]');
        if (transition) {
            gsap.set(transition, { scaleY: 0, transformOrigin: 'top' });
            gsap.fromTo(transition.querySelector('span'), { scaleX: 0 }, {
                scaleX: 1,
                duration: 1,
                ease: 'power3.inOut',
                onComplete: () => gsap.set(transition.querySelector('span'), { scaleX: 0 })
            });

            document.addEventListener('click', (event) => {
                const link = event.target.closest('a[href]');
                if (!link || !shouldTransition(link, event)) {
                    return;
                }

                event.preventDefault();
                transition.classList.add('is-active');
                gsap.fromTo(transition, { scaleY: 0, transformOrigin: 'bottom' }, {
                    scaleY: 1,
                    duration: .42,
                    ease: 'power3.inOut',
                    onComplete: () => window.location.assign(link.href)
                });
            });
        }

        const heroTimeline = gsap.timeline({ defaults: { ease: 'power3.out' } });
        heroTimeline
            .from('.hero__media img', { scale: 1.1, duration: 1.8 })
            .from('.hero-kicker', { y: 18, duration: .65 }, '-=1.15')
            .from('.hero h1', { y: 70, duration: 1.05 }, '-=.65')
            .from('.hero__lead, .hero__actions', { y: 22, stagger: .12, duration: .7 }, '-=.55')
            .from('.hero__index, .hero__signature, .scroll-cue', { y: 10, duration: .6 }, '-=.35');

        gsap.utils.toArray('.reveal:not([data-vehicle-card])').forEach((element) => {
            gsap.from(element, {
                scrollTrigger: { trigger: element, start: 'top 88%', once: true },
                y: 34,
                opacity: 0,
                duration: .82,
                ease: 'power2.out'
            });
        });

        gsap.utils.toArray('[data-card-grid]').forEach((grid) => {
            const cards = grid.querySelectorAll('[data-vehicle-card]');
            if (!cards.length) {
                return;
            }
            gsap.from(cards, {
                scrollTrigger: { trigger: grid, start: 'top 86%', once: true },
                y: 42,
                opacity: 0,
                stagger: .09,
                duration: .7,
                ease: 'power2.out'
            });
        });

        gsap.utils.toArray('.image-reveal').forEach((frame) => {
            gsap.fromTo(frame, { clipPath: 'inset(0 0 100% 0)' }, {
                clipPath: 'inset(0 0 0% 0)',
                duration: 1.15,
                ease: 'power3.inOut',
                scrollTrigger: { trigger: frame, start: 'top 84%', once: true }
            });
            const image = frame.querySelector('img');
            if (image) {
                gsap.from(image, {
                    scale: 1.08,
                    duration: 1.35,
                    ease: 'power3.out',
                    scrollTrigger: { trigger: frame, start: 'top 84%', once: true }
                });
            }
        });

        gsap.utils.toArray('.parallax-media img').forEach((image) => {
            gsap.to(image, {
                yPercent: -7,
                ease: 'none',
                scrollTrigger: { trigger: image.parentElement, start: 'top bottom', end: 'bottom top', scrub: .7 }
            });
        });

        document.querySelectorAll('[data-count]').forEach((node) => {
            const target = Number(node.dataset.count);
            const counter = { value: 0 };
            gsap.to(counter, {
                value: target,
                duration: 1.4,
                ease: 'power2.out',
                scrollTrigger: { trigger: node, start: 'top 90%', once: true },
                onUpdate: () => { node.textContent = String(Math.round(counter.value)); }
            });
        });

        document.querySelectorAll('[data-filter-form]').forEach((form) => {
            form.addEventListener('submit', (event) => {
                if (form.dataset.submitting === 'true') {
                    return;
                }
                const results = document.querySelector('[data-card-grid], .empty-state');
                if (!results) {
                    return;
                }
                event.preventDefault();
                form.dataset.submitting = 'true';
                gsap.to(results, {
                    opacity: 0,
                    y: 12,
                    duration: .2,
                    onComplete: () => form.submit()
                });
            });
        });
    }

    function shouldTransition(link, event) {
        if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
            return false;
        }
        if (link.target === '_blank' || link.hasAttribute('download')) {
            return false;
        }
        const destination = new URL(link.href, window.location.href);
        if (destination.origin !== window.location.origin) {
            return false;
        }
        const sameDocumentHash = destination.pathname === window.location.pathname
            && destination.search === window.location.search
            && destination.hash;
        return !sameDocumentHash;
    }
})();
