/**
 * Listing Form Dynamic Validation
 * Handles conditional website URL requirement, listing mode, and auction fields
 */
document.addEventListener('DOMContentLoaded', function() {
    const typeSelect = document.getElementById('type');
    const websiteUrlInput = document.getElementById('websiteUrl');
    const websiteUrlRequired = document.getElementById('websiteUrlRequired');
    const websiteUrlOptional = document.getElementById('websiteUrlOptional');
    const websiteUrlHelp = document.getElementById('websiteUrlHelp');
    const verificationNotice = document.getElementById('verificationNotice');
    
    const listingModeRadios = document.querySelectorAll('input[name="listingMode"]');
    const normalPriceSection = document.getElementById('normalPriceSection');
    const auctionSection = document.getElementById('auctionSection');
    const priceInput = document.getElementById('price');
    const priceAuctionInput = document.getElementById('priceAuction');
    const startingBidInput = document.getElementById('startingBid');
    const auctionDaysInput = document.getElementById('auctionDays');

    function updateWebsiteUrlRequirement() {
        if (!typeSelect || !websiteUrlInput) return;
        
        const selectedType = typeSelect.value;
        const isRequired = selectedType === 'WEBSITE' || selectedType === 'DOMAIN';

        if (isRequired) {
            websiteUrlInput.setAttribute('required', 'required');
            if (websiteUrlRequired) websiteUrlRequired.classList.remove('hidden');
            if (websiteUrlOptional) websiteUrlOptional.classList.add('hidden');
            if (websiteUrlHelp) websiteUrlHelp.classList.remove('hidden');
            if (verificationNotice) verificationNotice.style.display = 'block';
        } else {
            websiteUrlInput.removeAttribute('required');
            if (websiteUrlRequired) websiteUrlRequired.classList.add('hidden');
            if (websiteUrlOptional) websiteUrlOptional.classList.remove('hidden');
            if (websiteUrlHelp) websiteUrlHelp.classList.add('hidden');
            if (verificationNotice) verificationNotice.style.display = 'none';
        }
    }
    
    function updateListingMode() {
        const selectedMode = document.querySelector('input[name="listingMode"]:checked')?.value || 'NORMAL';
        
        if (selectedMode === 'AUCTION') {
            if (normalPriceSection) normalPriceSection.style.display = 'none';
            if (auctionSection) auctionSection.style.display = 'block';
            if (priceInput) priceInput.removeAttribute('required');
            if (startingBidInput) startingBidInput.setAttribute('required', 'required');
            if (auctionDaysInput) auctionDaysInput.setAttribute('required', 'required');
        } else {
            if (normalPriceSection) normalPriceSection.style.display = 'block';
            if (auctionSection) auctionSection.style.display = 'none';
            if (priceInput) priceInput.setAttribute('required', 'required');
            if (startingBidInput) startingBidInput.removeAttribute('required');
            if (auctionDaysInput) auctionDaysInput.removeAttribute('required');
        }
    }

    // Update on page load
    if (typeSelect) {
        updateWebsiteUrlRequirement();
        typeSelect.addEventListener('change', updateWebsiteUrlRequirement);
    }
    
    // Handle listing mode changes
    if (listingModeRadios.length > 0) {
        updateListingMode();
        listingModeRadios.forEach(radio => {
            radio.addEventListener('change', updateListingMode);
        });
    }
    
    // Sync price fields for auction mode
    if (priceInput && priceAuctionInput) {
        priceAuctionInput.addEventListener('input', function() {
            priceInput.value = this.value;
        });
    }
});
