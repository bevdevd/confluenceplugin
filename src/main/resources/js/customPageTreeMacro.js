function hide(element, id) {
    if(element.innerHTML == '+ ') {
        element.innerHTML = '- '
        document.getElementById(id).style.display = "block"
    } else {
        element.innerHTML = '+ '
        document.getElementById(id).style.display = "none"
    }
}