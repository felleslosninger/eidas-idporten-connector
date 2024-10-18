document.addEventListener("DOMContentLoaded", function () {
    var dropdown = document.querySelector('.country-selector__dropdown');
    var countryNameInput = document.getElementById('countryName');
    const nextButton = document.querySelector("button[name='action'][value='next']");

    function toggleNextButton() {
        nextButton.disabled = !countryNameInput.value.trim();
        nextButton.className = nextButton.disabled ? "button button--disabled" : "button";
    }

    // Listen for clicks within the dropdown
    dropdown.addEventListener('click', function (event) {
        // Ensure the click is on a list item or a list item child
        if (event.target.matches('.country-selector__list li, .country-selector__list li *')) {
            setTimeout(function () { // Timeout to allow for the value to be updated
                toggleNextButton();
            }, 100);
        }
    });
});

