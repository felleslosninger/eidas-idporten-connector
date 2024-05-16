document.addEventListener('DOMContentLoaded', function() {
    var eidSelected = false;
    var links = document.getElementsByClassName('country')[0].querySelectorAll('a');

    links.forEach(link => link.addEventListener('click', function(event) {
        if (eidSelected) {
            event.preventDefault();
        } else {
            eidSelected = true;
        }
    }));
});
