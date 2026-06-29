// Initialize Vercel Speed Insights
import { injectSpeedInsights } from '@vercel/speed-insights';

try {
  injectSpeedInsights({
    debug: false,
    beforeSend: (data) => {
      if (!data || !data.url) {
        console.warn('[Speed Insights] Skipping event with missing data');
        return null;
      }
      return data;
    }
  });
} catch (error) {
  console.error('[Speed Insights] Failed to initialize:', error.message);
}
