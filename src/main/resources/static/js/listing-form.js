/**
 * Listing Form Dynamic Validation
 * Handles conditional website URL requirement based on listing type
 */
document.addEventListener('DOMContentLoaded', function() {
    const typeSelect = document.getElementById('type');
    const websiteUrlInput = document.getElementById('websiteUrl');
    const websiteUrlRequired = document.getElementById('websiteUrlRequired');
    const websiteUrlOptional = document.getElementById('websiteUrlOptional');
    const websiteUrlHelp = document.getElementById('websiteUrlHelp');
    
    function updateWebsiteUrlRequirement() {
        if (!typeSelect || !websiteUrlInput) return;
        
        const selectedType = typeSelect.value;
        const isRequired = selectedType === 'WEBSITE' || selectedType === 'DOMAIN';
        
        if (isRequired) {
            websiteUrlInput.setAttribute('required', 'required');
            if (websiteUrlRequired) websiteUrlRequired.classList.remove('hidden');
            if (websiteUrlOptional) websiteUrlOptional.classList.add('hidden');
            if (websiteUrlHelp) websiteUrlHelp.classList.remove('hidden');
        } else {
            websiteUrlInput.removeAttribute('required');
            if (websiteUrlRequired) websiteUrlRequired.classList.add('hidden');
            if (websiteUrlOptional) websiteUrlOptional.classList.remove('hidden');
            if (websiteUrlHelp) websiteUrlHelp.classList.add('hidden');
        }
    }
    
    // Update on page load
    if (typeSelect) {
        updateWebsiteUrlRequirement();
        typeSelect.addEventListener('change', updateWebsiteUrlRequirement);
    }
});

