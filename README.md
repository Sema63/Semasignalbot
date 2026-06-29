# StockCrypto PRO - Telegram Web App

A Telegram Web App for stock and crypto tracking, now with Vercel Speed Insights integration.

## 🚀 Features

- Telegram Web App integration
- **Vercel Speed Insights** - Automatic Core Web Vitals tracking and performance monitoring

## 📊 Vercel Speed Insights

This project includes Vercel Speed Insights to track and monitor web performance metrics including:

- Core Web Vitals (LCP, FID, CLS)
- Time to First Byte (TTFB)
- First Contentful Paint (FCP)
- And more...

### How It Works

The project uses the `@vercel/speed-insights` package, which is bundled into a standalone JavaScript file and included in the HTML. The integration:

1. Automatically tracks Core Web Vitals
2. Sends performance metrics to Vercel
3. Works without any additional configuration
4. Only tracks in production (not in development)

### Configuration

Speed Insights is configured in `speed-insights.js` with the following options:

```javascript
injectSpeedInsights({
  debug: false, // Set to true to see events in console
  // Optional: sampleRate: 0.5 - Track only 50% of page views
  // Optional: beforeSend - Modify events before sending
});
```

To modify the configuration, edit `speed-insights.js` and run `npm run build`.

## 🛠️ Development

### Prerequisites

- Node.js (v18 or higher recommended)
- npm or pnpm

### Installation

```bash
# Install dependencies
npm install

# Build the Speed Insights bundle
npm run build
```

### Scripts

- `npm run build` - Bundles Speed Insights into dist/speed-insights.min.js
- `npm run dev` - Starts a local development server on port 8000
- `npm run lint` - Runs linting (currently not configured)

### Project Structure

```
.
├── index.html                  # Main HTML file with Speed Insights integration
├── speed-insights.js           # Speed Insights configuration
├── build-speed-insights.js     # Build script to bundle Speed Insights
├── dist/
│   └── speed-insights.min.js   # Bundled Speed Insights script
├── package.json                # Project dependencies and scripts
├── vercel.json                 # Vercel deployment configuration
└── README.md                   # This file
```

## 🌐 Deployment

This project is designed to be deployed on Vercel:

1. Push your code to GitHub
2. Import the project in Vercel
3. Vercel will automatically:
   - Install dependencies
   - Run the build command
   - Deploy your site with Speed Insights enabled

### Viewing Speed Insights Data

After deployment:

1. Go to your Vercel project dashboard
2. Navigate to the "Speed Insights" tab
3. View real-time performance metrics and Core Web Vitals

## 📝 Notes

- Speed Insights only tracks data in production, not during local development
- The tracking script is automatically initialized when the page loads
- No additional configuration is required in the Vercel dashboard
- The bundled script is approximately 1.5KB (minified)

## 🔧 Customization

To customize Speed Insights behavior:

1. Edit `speed-insights.js`
2. Add options like:
   - `sampleRate`: Control what percentage of page views to track
   - `beforeSend`: Filter or modify events before sending
   - `debug`: Enable console logging
3. Run `npm run build` to regenerate the bundle
4. Deploy to Vercel

Example with custom options:

```javascript
injectSpeedInsights({
  debug: true,
  sampleRate: 0.5, // Track 50% of page views
  beforeSend: (data) => {
    // Filter sensitive routes
    if (data.url.includes('/admin')) {
      return null; // Don't track this event
    }
    return data;
  }
});
```

## 📚 Resources

- [Vercel Speed Insights Documentation](https://vercel.com/docs/speed-insights)
- [@vercel/speed-insights Package](https://www.npmjs.com/package/@vercel/speed-insights)
- [Web Vitals](https://web.dev/vitals/)

## 📄 License

This project is private.
