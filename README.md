# Coeus

Coeus is an Android application for sending NFC APDU commands, named after the Greek Titan of intellect.

This project implements Clean Architecture with three distinct layers: data, domain, and presentation. It uses Material 3 design for the user interface and is built with Kotlin, Jetpack Compose, and Gradle with Kotlin DSL. The project also features ViewBinding and follows modern Android development practices.

## Architecture

The project is organized following Clean Architecture principles:

- **Data Layer**: Contains data sources, repositories, and data models
- **Domain Layer**: Contains business logic, use cases, and entities
- **Presentation Layer**: Contains UI components, ViewModels, and UI-related logic

## Features

- NFC APDU command sending functionality
- Modern UI with Jetpack Compose and Material 3
- Clean Architecture implementation
- ViewBinding enabled
- Kotlin-based development

## Dependencies

- AndroidX Core
- Jetpack Compose (BOM)
- Material Design 3
- Kotlin Coroutines
- Lifecycle ViewModel
- AndroidX NFC
