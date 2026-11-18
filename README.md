# Thai FDA Image Checker

An AI-powered application for detecting and verifying Thai FDA registration numbers from product label images.

## Overview

Thai FDA Image Checker is an application designed to help users check product registration information by uploading an image of a product label.

The system uses object detection and Optical Character Recognition (OCR) to identify Thai FDA registration numbers, then retrieves product information from the Thai FDA website.

## Objectives

- Detect Thai FDA registration number regions from product label images.
- Extract registration numbers using OCR technology.
- Verify registration numbers against Thai FDA information.
- Provide an easy-to-use interface for checking product information.

## System Workflow

1. **Image Upload** – Users upload an image of a product label.
2. **Object Detection** – YOLO detects the FDA registration number region.
3. **Image Processing** – The detected region is cropped and prepared for OCR.
4. **Text Recognition** – OCR extracts the registration number.
5. **FDA Verification** – The system searches for matching information from the Thai FDA website.
6. **Result Display** – The application displays the verification result and product details.

## Technologies

| Component | Technology |
|---|---|
| Mobile Application | Android |
| Programming Language | Kotlin |
| Backend API | Python, FastAPI |
| Object Detection | YOLO |
| OCR | GPT-based OCR / EasyOCR / Tesseract |
| API Communication | REST API |
| Data Retrieval | Thai FDA Website |

## Key Features

- Upload product label images.
- Detect FDA registration number regions using YOLO.
- Extract text from detected regions using OCR.
- Retrieve product information from the Thai FDA website.
- Display verification results.
- User registration and login.
- View checking history.

## My Contributions

My responsibilities in this project include:

- **Backend Development:** Developed REST API endpoints using FastAPI.
- **Object Detection:** Integrated YOLO for detecting Thai FDA registration number regions.
- **OCR Integration:** Implemented OCR processing to extract registration numbers.
- **API Integration:** Connected the Android application with the backend API.
- **UI/UX Design:** Contributed to the application interface and user experience.


*Note: The structure above is a simplified overview of the project.*

## Project Status

Research and development project.

---

**Thai FDA Image Checker | Computer Vision | AI | Mobile Application**
