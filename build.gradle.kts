// Root project: requirement tracing across all modules (MADR 0001).
// Code lives in :chatpane (the library) and :demo; shared Java setup in build-logic/.

plugins {
    id("org.itsallcode.openfasttrace") version "3.2.0"
}

requirementTracing {
    inputDirectories = files("docs/requirements", "chatpane/src", "demo/src")
}
