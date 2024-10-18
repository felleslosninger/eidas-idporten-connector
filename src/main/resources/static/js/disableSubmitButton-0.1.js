document.addEventListener('DOMContentLoaded', function() {
    var formSubmitted = false;
    var form = document.getElementById('countryForm');

    form.addEventListener('submit', function(event) {
        if (formSubmitted) {
            event.preventDefault();
            console.log('Form has already been submitted');
        } else {
            formSubmitted = true;
        }
    });
});