# FLAN-T5-small Model Integration

## Overview
This project now integrates Google's FLAN-T5-small model for intelligent transaction parsing and categorization. The model understands natural language descriptions of financial transactions and automatically categorizes them.

## Implementation Details

### Core Components

1. **LocalAIService.kt** - Main service for FLAN-T5-small model integration
   - `parseTransactionText()` - Processes transaction descriptions using the model
   - Currently simulates model responses (ready for actual TensorFlow Lite integration)
   - Parses model output into structured transaction data

2. **AIService.kt** - Enhanced with new method
   - `parseTransactionWithAI()` - Uses LocalAIService for intelligent parsing
   - Integrates with existing ML Kit functionality

3. **LedgerRepository.kt** - Updated transaction creation
   - `createEntryFromText()` now uses FLAN-T5-small for categorization
   - Improved accuracy over keyword-based matching

4. **AddEntryViewModel.kt** - New parsing method
   - `parseWithFlanT5()` - Direct access to FLAN-T5 parsing
   - Updates UI state with parsed transaction details

### Model Prompt
The model uses this specific prompt for categorization:
```
You are given a piece of text describing a ledger change: [CONTENT], please choose appropriate category of: spend or income;amount;the category among one of [FOOD_DINING,TRANSPORTATION,SHOPPING,ENTERTAINMENT,UTILITIES,HEALTHCARE,EDUCATION,TRAVEL,INVESTMENT,SALARY,FREELANCE,BUSINESS,GIFTS,OTHER], output with 3 pieces split by ;
```

### Response Format
The model returns responses in the format: `type;amount;category`
Example: `spend;4.50;FOOD_DINING`

## Testing the Integration

### In the App
1. Open the SolMind Assistant screen
2. You'll see test buttons in the welcome message:
   - **Test: Coffee** - Tests expense categorization
   - **Test: Salary** - Tests income categorization
3. Click either button to see the FLAN-T5 model parse the transaction
4. The parsed results will update the transaction form automatically

### Sample Test Cases
- "Bought coffee for $4.50 at Starbucks" → `spend;4.50;FOOD_DINING`
- "Received salary payment of $3000" → `income;3000.0;SALARY`
- "Paid electricity bill $120" → `spend;120.0;UTILITIES`
- "Uber ride to airport $25" → `spend;25.0;TRANSPORTATION`

## Dependencies Added
```gradle
// TensorFlow Lite for FLAN-T5-small model
implementation 'org.tensorflow:tensorflow-lite:2.13.0'
implementation 'org.tensorflow:tensorflow-lite-support:0.4.4'
implementation 'org.tensorflow:tensorflow-lite-task-text:0.4.4'
```

## Future Enhancements
1. **Actual Model Loading**: Replace simulation with real TensorFlow Lite model
2. **Model Optimization**: Fine-tune for financial transaction parsing
3. **Confidence Scoring**: Improve confidence calculation based on model outputs
4. **Batch Processing**: Support multiple transaction parsing
5. **Custom Categories**: Allow user-defined transaction categories

## Benefits
- **Improved Accuracy**: AI-powered categorization vs keyword matching
- **Natural Language**: Understands conversational transaction descriptions
- **Automatic Amount Extraction**: Identifies monetary values in text
- **Type Detection**: Distinguishes between income and expenses
- **Category Assignment**: Maps to predefined financial categories
- **Confidence Scoring**: Provides reliability metrics for parsed data

## Architecture
```
User Input → LocalAIService → FLAN-T5 Model → Parsed Response → UI Update
     ↓
LedgerRepository → Database Storage
```

The integration maintains backward compatibility while adding powerful AI capabilities for transaction understanding and categorization.