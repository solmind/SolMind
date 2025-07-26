# AI Ledger for Solana Mobile

An AI-powered expense tracking Android app built for the Solana Mobile Hackathon. This app combines artificial intelligence with blockchain technology to provide intelligent expense categorization and automatic Solana transaction tracking.

## Recent Updates (Latest)

### 🔮 Cloud AI Model "COMING" Feature
- **Premium Model Preview**: Cloud AI models now marked as "COMING" with visual badges
- **Upgrade Flow Integration**: Clicking cloud models redirects users to SolMind Master upgrade page
- **Visual Distinction**: Cloud models display with "COMING" badge and CloudQueue icon
- **Unselectable State**: Cloud models are disabled until subscription upgrade
- **Clear User Guidance**: Intuitive UI guides users toward premium subscription benefits
- **Subscription Awareness**: Seamless integration with existing upgrade dialog system

### 💰 Currency Preference System & Account Mode Integration
- **Smart Currency Display**: Comprehensive currency preference system with USD and SOL support
- **Account Mode Awareness**: Offchain mode always uses USD, onchain mode allows user preference
- **Persistent Preferences**: Currency settings saved using DataStore for seamless user experience
- **Context-Aware Settings**: Currency preference option only appears in onchain mode
- **Universal Integration**: Currency formatting applied across all screens (Home, Add Entry, Settings)
- **Real-time Updates**: Instant UI updates when switching currency preferences
- **Professional Formatting**: Proper currency symbols and formatting for both USD ($) and SOL (◎)

### 🎯 Enhanced Transaction Experience & UI Improvements
- **Smart Category Dropdown**: AI-powered category suggestions with custom input support
- **Seamless Navigation**: Fixed navigation flow to keep users on chatbot interface after saving transactions
- **Enhanced User Experience**: Streamlined transaction editing and saving process
- **Custom Category Support**: Users can input custom categories with AI-powered suggestions
- **Improved Conversation Flow**: Maintains chat context throughout transaction lifecycle

### 🚀 LiteRT Model Integration & UI Improvements
- **Model Replacement**: Migrated from legacy models to LiteRT-compatible versions from `litert-community`
- **Enhanced UI**: Fixed model list scrolling with LazyColumn implementation
- **Improved Size Display**: Resolved "Unknown" size issues with intelligent fallback estimates
- **Better Error Handling**: Enhanced model size detection with multiple API strategies
- **Edge-Optimized**: All models now pre-converted for efficient edge deployment

## Features

### 🤖 AI-Powered Expense Classification
- **Smart Text Recognition**: Extract transaction details from receipt photos using ML Kit
- **LiteRT-Compatible Models**: Choose from optimized edge-deployment models:
  - **TinyLlama 1.1B Chat**: Compact conversational AI model (~2.2 GB)
  - **Gemma3 1B IT**: Google's instruction-tuned model (~2.5 GB)
  - **Phi-4 Mini Instruct**: Microsoft's efficient instruction model (~7.4 GB)
  - **SmolLM 1.7B**: HuggingFace's compact language model (~3.4 GB)
- **Cloud AI Models**: Access to SolMind Cloud AI with subscription (SolMind Master)
- **Enhanced Model Management**: 
  - Scrollable model selection interface
  - Real-time model size preview
  - Download progress tracking
  - Intelligent size estimation for all models
- **Comprehensive Field Inference**: Automatically extract 5 key fields from user input:
  - Transaction type (income/expense)
  - Amount (with enhanced pattern recognition)
  - Category (intelligent keyword matching)
  - Description (cleaned and formatted from input)
  - Date (parsed from text or defaults to current date)
- **Enhanced Amount Extraction**: Advanced regex patterns for detecting amounts in various formats ($123.45, 123.45$, etc.)
- **Smart Description Generation**: Intelligently cleans input text to create meaningful transaction descriptions
- **Flexible Date Parsing**: Supports multiple date formats (YYYY-MM-DD, MM/DD/YYYY, MM-DD-YYYY)
- **Intelligent Categorization**: Automatically categorize expenses using selected AI models with improved keyword matching
- **Confidence Scoring**: Display AI confidence levels for categorization accuracy
- **Manual Override**: Easy manual editing and correction of AI suggestions
- **Offline AI**: All AI processing runs locally on device for privacy

### 💎 SolMind Master Subscription
- **Premium Tier**: Upgrade to SolMind Master for enhanced features
- **Cloud AI Access**: Unlock powerful cloud-based AI models for better accuracy
- **Development Support**: Support ongoing development and new features
- **Future Features**: Access to upcoming pro functionality
- **Special Launch Pricing**: Free upgrade for early adopters
- **Seamless Integration**: Easy upgrade flow directly from settings

### 🔗 Solana Blockchain Integration
- **Wallet Management**: Add and manage multiple Solana wallet addresses
- **Automatic Sync**: Real-time synchronization of on-chain transactions
- **Transaction Parsing**: Intelligent parsing of Solana transaction data
- **Balance Tracking**: Monitor SOL balances across connected wallets

### 📱 Modern Android UI
- **Material 3 Design**: Beautiful, modern interface following Material Design guidelines
- **Jetpack Compose**: Built with the latest Android UI toolkit
- **Advanced Theme System**: Separate theme preferences for on-chain and off-chain modes
- **Animated Mode Switcher**: Smooth transitions between on-chain and off-chain modes
- **Multiple Theme Options**: Light, Dark, and System theme modes for each account type
- **Responsive Layout**: Optimized for various screen sizes

### 📊 Comprehensive Tracking
- **Multiple Categories**: Food, Transportation, Shopping, Entertainment, Bills, Healthcare, Education, Investment, and more
- **Income & Expense Tracking**: Complete financial overview
- **Monthly Summaries**: Detailed monthly income and expense breakdowns
- **Search & Filter**: Advanced filtering by category, date, and amount

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Repository Pattern
- **Dependency Injection**: Hilt
- **Database**: Room (SQLite)
- **Preferences**: DataStore for theme and user preferences
- **AI/ML**: ML Kit for text recognition, PyTorch/ExecuTorch for local AI models
- **Blockchain**: Solana Web3.js integration
- **Camera**: CameraX
- **Navigation**: Jetpack Navigation Compose

## Project Structure

```
app/src/main/java/com/solana/ailedger/
├── data/
│   ├── database/          # Room database, DAOs, and entities
│   ├── model/            # Data models and entities
│   ├── repository/       # Repository pattern implementation
│   └── service/          # AI and Solana services
├── di/                   # Dependency injection modules
├── ui/
│   ├── screens/          # Compose UI screens
│   ├── theme/            # Material 3 theming
│   └── viewmodel/        # ViewModels for UI state management
└── AILedgerApplication.kt # Application class
```

## Key Components

### AI Service (`AIService.kt` & `LocalAIService.kt`)
- Text extraction from images using ML Kit
- Local AI model inference using PyTorch/ExecuTorch
- **Enhanced Transaction Parsing**: Comprehensive field extraction including:
  - Transaction type inference (income vs expense)
  - Advanced amount extraction with multiple regex patterns
  - Intelligent category matching with expanded keyword sets
  - Smart description generation from cleaned input text
  - Flexible date parsing supporting various formats
- Transaction categorization using selected language models
- Confidence scoring for AI predictions based on field completeness
- Helper functions for `extractDescription()` and `extractDate()`

### Model Manager (`ModelManager.kt`)
- **LiteRT Model Integration**: Seamless integration with `litert-community` models from HuggingFace
- **Enhanced Size Detection**: Intelligent model size fetching with multiple fallback strategies
- **Improved Error Handling**: Robust error handling with estimated size fallbacks
- **Model Download Progress**: Real-time download progress tracking with status updates
- **Optimized Edge Models**: Support for TinyLlama, Gemma3, Phi-4 Mini, and SmolLM models
- **Cloud AI Integration**: SolMind Cloud AI model access for premium users
- **Smart Caching**: Efficient model size caching to reduce API calls

### Currency Preference Manager (`CurrencyPreferenceManager.kt`)
- **Currency Display Management**: Handle user preferences for USD vs SOL display
- **DataStore Integration**: Persistent storage of currency preferences using Android DataStore
- **Account Mode Integration**: Automatic USD enforcement for offchain mode
- **Real-time Updates**: Flow-based reactive currency preference updates
- **Suspend Functions**: Async currency preference retrieval for ViewModels

### Currency Formatter (`CurrencyFormatter.kt`)
- **Dual Currency Support**: Professional formatting for both USD ($) and SOL (◎) currencies
- **Account Mode Awareness**: Automatically enforces USD formatting for offchain transactions
- **Flexible Formatting**: Supports both regular amounts and transaction-specific formatting
- **Consistent Display**: Ensures uniform currency presentation across the entire app
- **Precision Handling**: Proper decimal places for USD (2) and SOL (4) currencies

### Subscription Manager (`SubscriptionManager.kt`)
- SolMind Master subscription tier management
- Upgrade and cancellation functionality
- Subscription status tracking
- Benefits and pricing management
- Integration with cloud AI model access

### Solana Service (`SolanaService.kt`)
- Solana RPC integration
- Transaction history retrieval
- Balance checking
- Address validation

### Database Layer
- **LedgerEntry**: Core transaction data model
- **SolanaWallet**: Wallet management
- **Room Database**: Local data persistence

### UI Screens
- **HomeScreen**: Dashboard with balance overview and recent transactions
- **AddEntryScreen**: Manual transaction entry with AI assistance and integrated camera functionality
- **WalletScreen**: Solana wallet management
- **SettingsScreen**: App configuration, preferences, and AI model management

## Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24 (API level 24) or higher
- Kotlin 1.8.0 or later

### Installation

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd solana-hackerthon
   ```

2. Open the project in Android Studio

3. Sync the project with Gradle files

4. Build and run the app:
   ```bash
   ./gradlew build
   ./gradlew installDebug
   ```

### Configuration

1. **Android SDK**: Ensure Android SDK is properly configured in `local.properties`
2. **Permissions**: The app requires camera and internet permissions
3. **Solana Network**: Currently configured for Solana mainnet

## Usage

### Adding Transactions

1. **Manual Entry**: Use the "Add Entry" screen to manually input transaction details
2. **Camera Capture**: Use the integrated camera feature in the "Add Entry" screen to take photos of receipts and let AI extract the details
3. **AI Assistance**: Get intelligent suggestions for categorization

### Managing Wallets

1. **Add Wallet**: Enter a Solana wallet address and give it a name
2. **Sync Transactions**: Automatically fetch and categorize on-chain transactions
3. **Monitor Balance**: Track SOL balances across all connected wallets

### Viewing Reports

1. **Dashboard**: View monthly income/expense summaries
2. **Transaction History**: Browse all transactions with filtering options
3. **Category Breakdown**: Analyze spending patterns by category

## Contributing

This project was built for the Solana Mobile Hackathon. Contributions and improvements are welcome!

### Development Guidelines

1. Follow Kotlin coding conventions
2. Use Jetpack Compose for UI development
3. Implement proper error handling
4. Add unit tests for business logic
5. Follow Material Design guidelines

## License

This project is open source and available under the [MIT License](LICENSE).

## Acknowledgments

- Built for the Solana Mobile Hackathon
- Powered by Solana blockchain technology
- Uses Google ML Kit for AI capabilities
- Designed with Material 3 guidelines

## Contact

For questions or support, please open an issue in the repository.

---

**Built with ❤️ for the Solana Mobile Hackathon**