source "https://rubygems.org"

# Release tooling. Always invoke via `bundle exec`, never system-wide fastlane.
gem "fastlane", "~> 2.227"

# Loads fastlane plugins declared in fastlane/Pluginfile.
plugins_path = File.join(File.dirname(__FILE__), "fastlane", "Pluginfile")
eval_gemfile(plugins_path) if File.exist?(plugins_path)
