# Language Model Selection Feature

## Overview
This document describes the implementation of the local language model selection feature in the SolMind app settings page. Users can now select, download, and manage different AI models for transaction parsing.

## Implementation Components

### 1. ModelManager.kt
- **Location**: `app/src/main/java/com/solana/solmind/service/ModelManager.kt`
- **Purpose**: Manages local language models including selection, download, and status tracking
- **Key Features**:
  - Model selection and persistence
  - Download progress tracking
  - Model deletion functionality
  - Available models: FLAN-T5 Small (default), DialoGPT Small, DistilBERT Base

### 2. SettingsScreen.kt Updates
- **Location**: `app/src/main/java/com/solana/solmind/ui/screens/SettingsScreen.kt`
- **New Features**:
  - Language Model selection in AI Settings section
  - ModelSelectionDialog for model management
  - ModelItem component for individual model display
  - Download/Delete buttons based on model status
  - Progress indicators for downloading models

### 3. LocalAIService.kt Integration
- **Location**: `app/src/main/java/com/solana/solmind/service/LocalAIService.kt`
- **Updates**:
  - Integrated with ModelManager for dynamic model selection
  - Uses selected model for transaction parsing
  - Fallback to simulation when model not available

## User Interface Features

### Settings Page Integration
- **AI Settings Section**: New "Language Model" option
- **Current Model Display**: Shows selected model name and download status
- **Model Selection Dialog**: Comprehensive model management interface

### Model Selection Dialog
- **Model List**: Displays all available models with details
- **Download Status**: Shows current status (Not Downloaded, Downloading, Downloaded, Error)
- **Progress Tracking**: Real-time download progress with percentage
- **Action Buttons**:
  - Download button for undownloaded models
  - Delete button for downloaded models (except selected)
  - Radio button selection for downloaded models

### Model Information Display
- **Model Name**: Clear identification of each model
- **Description**: Brief explanation of model capabilities
- **Size**: Storage requirements for each model
- **Default Badge**: Indicates the default recommended model

## Available Models

### 1. FLAN-T5 Small (Default)
- **ID**: `google/flan-t5-small`
- **Size**: ~80MB
- **Description**: Google's instruction-tuned T5 model, optimized for following instructions
- **Use Case**: General-purpose text understanding and generation

### 2. DialoGPT Small
- **ID**: `microsoft/DialoGPT-small`
- **Size**: ~117MB
- **Description**: Microsoft's conversational AI model
- **Use Case**: Conversational responses and dialogue generation

### 3. DistilBERT Base
- **ID**: `distilbert-base-uncased`
- **Size**: ~66MB
- **Description**: Lightweight version of BERT for text classification
- **Use Case**: Text classification and understanding tasks

## Technical Implementation

### Model State Management
```kotlin
data class ModelState(
    val model: LanguageModel,
    val status: ModelDownloadStatus,
    val progress: Float = 0f
)

enum class ModelDownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    ERROR
}
```

### Download Management
- **Progress Tracking**: Real-time download progress updates
- **Error Handling**: Graceful error handling with retry options
- **Storage Management**: Efficient local storage of model files
- **Background Downloads**: Non-blocking download operations

### Model Selection Persistence
- **SharedPreferences**: Stores selected model preference
- **State Restoration**: Maintains selection across app restarts
- **Default Fallback**: Automatically selects FLAN-T5 Small if no preference set

## Usage Instructions

### Accessing Model Selection
1. Open the SolMind app
2. Navigate to Settings
3. Scroll to "AI Settings" section
4. Tap on "Language Model"

### Downloading a Model
1. In the Model Selection Dialog, find your desired model
2. Tap the "Download" button
3. Wait for download completion (progress shown)
4. Model becomes available for selection

### Selecting a Model
1. Ensure the model is downloaded
2. Tap the radio button next to the model name
3. The model is immediately selected for use
4. Tap "Done" to close the dialog

### Deleting a Model
1. Find a downloaded model (not currently selected)
2. Tap the delete icon
3. Model is removed from local storage
4. Can be re-downloaded if needed later

## Integration with AI Features

### Transaction Parsing
- Selected model is used for `parseTransactionWithAI` function
- Automatic fallback to simulation if model unavailable
- Consistent API regardless of selected model

### Chat Interface
- FLAN-T5 test buttons continue to work
- Uses currently selected model for inference
- Maintains backward compatibility

## Benefits

### User Control
- **Model Choice**: Users can select models based on their preferences
- **Storage Management**: Download only needed models to save space
- **Performance Optimization**: Choose models based on device capabilities

### Flexibility
- **Multiple Options**: Support for various model types and sizes
- **Easy Switching**: Change models without app restart
- **Future Expansion**: Framework ready for additional models

### Offline Capability
- **Local Inference**: All models run locally on device
- **No Internet Required**: Works without network connectivity
- **Privacy Focused**: No data sent to external servers

## Future Enhancements

### Planned Features
- **Model Performance Metrics**: Compare accuracy and speed
- **Custom Model Support**: Allow users to import their own models
- **Automatic Updates**: Update models when new versions available
- **Model Recommendations**: Suggest optimal models based on usage patterns

### Technical Improvements
- **Model Compression**: Reduce model sizes for faster downloads
- **Incremental Updates**: Download only model differences
- **Background Processing**: Optimize model loading and inference
- **Memory Management**: Better handling of large models

## Architecture Benefits

### Modular Design
- **Separation of Concerns**: Clear separation between UI, management, and inference
- **Testability**: Easy to test individual components
- **Maintainability**: Clean code structure for future updates

### Scalability
- **Easy Model Addition**: Simple process to add new models
- **Configuration Driven**: Model properties defined in data classes
- **Plugin Architecture**: Ready for advanced model management features

This implementation provides a solid foundation for local AI model management while maintaining the app's focus on privacy and offline functionality.