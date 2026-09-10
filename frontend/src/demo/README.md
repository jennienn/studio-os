# Isolated visual prototype

This directory contains legacy, browser-only demo state, fixtures and screens.
It is not production authentication, authorization, persistence or business logic.
The `(demo)` route layout requires `NEXT_PUBLIC_ENABLE_DEMO=true` at build/dev time.
The default build displays a service-unavailable preview notice on those routes.
Never enter real customer information. Demo state uses a separate localStorage key.
Do not extend the legacy business handlers. Future domain behavior belongs in Spring.
Shared landing components remain outside this directory.
