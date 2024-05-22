document.addEventListener('DOMContentLoaded', function() {
    var formSubmitted = false;
    var form = document.getElementById('consent-form');

    form.addEventListener('submit', function(event) {
        if (formSubmitted) {
            event.preventDefault();
        } else {
            formSubmitted = true;
        }
    });
});