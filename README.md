# PA24-ApplicationAndroid

## Description

The android app for the ParisCareTaker project, using Kotlin and XML

## Features

- Consult and reserve a house
- Consult your messages with sellers
- Consult your reservations
- Consult your profile

## Installation

The application is only available via Github. To install:
- Clone the repository
- Open the project with Android Studio
- Create a local.properties file in the main/assets folder with the content of local.properties.example
- Build and run the application on an emulator or a physical device

## Use

1. Login through the login menu that will be display the first time you open the app.
2. On the bottom of the application, you can use the footer to navigate between the main menu, the travel page, the messages page, and the profile page, where you can use the NFC, display your profile, and log out.

## Dependencies

ParisCareTaker uses the following libraries and services:

1. **Google Map API** : For map integration and location services.
2. **Google Place API**: For place search and selection.
3. **NDEF** For Near Field Communication (NFC).
4. **Stripe**: For payment processing.

