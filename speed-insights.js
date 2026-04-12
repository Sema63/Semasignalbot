// Initialize Vercel Speed Insights
// This script is loaded from the node_modules and initializes Speed Insights tracking
import { injectSpeedInsights } from '@vercel/speed-insights';

// Inject Speed Insights with default configuration
// Speed Insights will automatically track Core Web Vitals and performance metrics
injectSpeedInsights({
  debug: false, // Set to true for debugging in console
  // You can add more configuration options here:
  // sampleRate: 1, // Set to 0.5 for 50% sampling
  // beforeSend: (data) => {
  //   // Modify or filter events before sending
  //   return data;
  // }
});
