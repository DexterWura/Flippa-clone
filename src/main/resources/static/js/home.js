/**
 * Home Page Typing Animation
 * Creates a typing effect for rotating words
 */
document.addEventListener('DOMContentLoaded', function() {
    const words = ['online businesses', 'social accounts', 'domains'];
    const el = document.getElementById('typed');
    if (!el) return;
    
    let wordIndex = 0, charIndex = 0, deleting = false;
    const typeSpeed = 70;
    const deleteSpeed = 45;
    const holdTime = 1200;

    function typeLoop() {
        const current = words[wordIndex];
        if (!deleting) {
            el.textContent = current.slice(0, ++charIndex);
            if (charIndex === current.length) {
                deleting = true;
                setTimeout(typeLoop, holdTime);
                return;
            }
        } else {
            el.textContent = current.slice(0, --charIndex);
            if (charIndex === 0) {
                deleting = false;
                wordIndex = (wordIndex + 1) % words.length;
            }
        }
        setTimeout(typeLoop, deleting ? deleteSpeed : typeSpeed);
    }
    
    typeLoop();
});

