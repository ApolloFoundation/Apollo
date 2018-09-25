var eventSource = new EventSource("http://127.0.0.1:7876/blocks?account=9211698109297098287");
console.log(eventSource);
eventSource.onopen = function() {
    NRS.logConsole("SSE onopen");
    console.log("SSE onopen");
}

eventSource.onmessage = function(event) {
    NRS.logConsole("SSE onmessage: " + event.data);
    console.log("SSE onmessage: " + event.data);
}
eventSource.onerror = function(event) {
    NRS.logConsole("SSE onerror " + event);
    eventSource.close();
}