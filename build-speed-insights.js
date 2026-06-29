// Build script to bundle Speed Insights for vanilla HTML
import * as esbuild from 'esbuild';
import { fileURLToPath } from 'url';
import { dirname } from 'path';

const __dirname = dirname(fileURLToPath(import.meta.url));

try {
  const result = await esbuild.build({
    entryPoints: ['speed-insights.js'],
    bundle: true,
    minify: true,
    format: 'iife',
    outfile: 'dist/speed-insights.min.js',
    platform: 'browser',
    target: ['es2020'],
    logLevel: 'info'
  });

  if (result.errors.length > 0) {
    console.error('Build completed with errors:');
    for (const error of result.errors) {
      console.error(`  ${error.text} (${error.location?.file}:${error.location?.line})`);
    }
    process.exit(1);
  }

  if (result.warnings.length > 0) {
    console.warn('Build warnings:');
    for (const warning of result.warnings) {
      console.warn(`  ${warning.text}`);
    }
  }

  console.log('Speed Insights bundled successfully!');
} catch (error) {
  console.error('Build failed:', error.message);
  if (error.errors) {
    for (const err of error.errors) {
      console.error(`  ${err.text}`);
    }
  }
  process.exit(1);
}
