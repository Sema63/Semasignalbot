// Build script to bundle Speed Insights for vanilla HTML
import * as esbuild from 'esbuild';
import { fileURLToPath } from 'url';
import { dirname } from 'path';

const __dirname = dirname(fileURLToPath(import.meta.url));

await esbuild.build({
  entryPoints: ['speed-insights.js'],
  bundle: true,
  minify: true,
  format: 'iife',
  outfile: 'dist/speed-insights.min.js',
  platform: 'browser',
  target: ['es2020'],
  logLevel: 'info'
});

console.log('✅ Speed Insights bundled successfully!');
