#!/usr/bin/env ruby

require 'fileutils'
require 'pathname'
require 'xcodeproj'

ROOT = Pathname.new(__dir__).parent.expand_path
PROJECT_PATH = ROOT + 'NexusPlatformApp.xcodeproj'
SOURCE_ROOT = ROOT + 'NexusPlatform' + 'Sources' + 'NexusPlatform'
INFO_PLIST_RELATIVE_PATH = 'NexusPlatformApp/Info.plist'
DEPLOYMENT_TARGET = '18.0'
PROJECT_NAME = 'NexusPlatformApp'

def sanitize_bundle_suffix(value)
  cleaned = value.to_s.gsub(/[^A-Za-z0-9]/, '').downcase
  cleaned.empty? ? 'local' : cleaned
end

def add_swift_sources(group, source_dir, target)
  file_refs = []

  source_dir.children.sort_by(&:to_s).each do |entry|
    next if entry.basename.to_s.start_with?('.')

    if entry.directory?
      subgroup = group.new_group(entry.basename.to_s, entry.basename.to_s)
      add_swift_sources(subgroup, entry, target)
      next
    end

    next unless entry.extname == '.swift'

    file_refs << group.new_file(entry.basename.to_s)
  end

  target.add_file_references(file_refs) unless file_refs.empty?
end

def add_package_dependency(project, target, url:, version:, product_name:)
  checkout_path = ROOT + '.build' + 'checkouts' + product_name

  if checkout_path.exist?
    package = project.new(Xcodeproj::Project::Object::XCLocalSwiftPackageReference)
    package.path = checkout_path.to_s
    package.relative_path = checkout_path.relative_path_from(ROOT).to_s
  else
    package = project.new(Xcodeproj::Project::Object::XCRemoteSwiftPackageReference)
    package.repositoryURL = url
    package.requirement = {
      'kind' => 'upToNextMajorVersion',
      'minimumVersion' => version,
    }
  end

  project.root_object.package_references << package

  product = project.new(Xcodeproj::Project::Object::XCSwiftPackageProductDependency)
  product.package = package
  product.product_name = product_name
  target.package_product_dependencies << product

  dependency = project.new(Xcodeproj::Project::Object::PBXTargetDependency)
  dependency.product_ref = product
  target.dependencies << dependency

  build_file = project.new(Xcodeproj::Project::Object::PBXBuildFile)
  build_file.product_ref = product
  target.frameworks_build_phase.files << build_file
end

FileUtils.rm_rf(PROJECT_PATH) if PROJECT_PATH.exist?

bundle_suffix = sanitize_bundle_suffix(ENV.fetch('USER', 'local'))
default_bundle_id = "com.nexusplatform.#{bundle_suffix}.host"
default_backend_url = ENV.fetch('PLATFORM_API_BASE_URL', ENV.fetch('BACKEND_BASE_URL', '')).strip
if default_backend_url.empty?
  abort <<~MSG
    Missing PLATFORM_API_BASE_URL.
    Generate the host project with an explicit backend API base URL, for example:
    PLATFORM_API_BASE_URL=http://<your-mac-lan-ip>:8080/api/v1 ruby scripts/generate_host_project.rb
  MSG
end
product_bundle_identifier = ENV.fetch('PRODUCT_BUNDLE_IDENTIFIER', default_bundle_id)
development_team = ENV['DEVELOPMENT_TEAM']

project = Xcodeproj::Project.new(PROJECT_PATH.to_s)
project.root_object.attributes['LastUpgradeCheck'] = '2600'
project.root_object.attributes['TargetAttributes'] ||= {}

project.build_configurations.each do |config|
  config.build_settings['SWIFT_VERSION'] = '5.0'
  config.build_settings['IPHONEOS_DEPLOYMENT_TARGET'] = DEPLOYMENT_TARGET
  config.build_settings['DEVELOPMENT_TEAM'] = development_team if development_team && !development_team.empty?
end

target = project.new_target(:application, PROJECT_NAME, :ios, DEPLOYMENT_TARGET)
target.product_name = PROJECT_NAME

project.root_object.attributes['TargetAttributes'][target.uuid] = {
  'CreatedOnToolsVersion' => '26.4',
  'ProvisioningStyle' => 'Automatic',
}

target.build_configurations.each do |config|
  settings = config.build_settings
  settings['PRODUCT_NAME'] = PROJECT_NAME
  settings['PRODUCT_BUNDLE_IDENTIFIER'] = product_bundle_identifier
  settings['INFOPLIST_FILE'] = INFO_PLIST_RELATIVE_PATH
  settings['GENERATE_INFOPLIST_FILE'] = 'NO'
  settings['SWIFT_VERSION'] = '5.0'
  settings['IPHONEOS_DEPLOYMENT_TARGET'] = DEPLOYMENT_TARGET
  settings['CODE_SIGN_STYLE'] = 'Automatic'
  settings['MARKETING_VERSION'] = '1.0'
  settings['CURRENT_PROJECT_VERSION'] = '1'
  settings['TARGETED_DEVICE_FAMILY'] = '1,2'
  settings['LD_RUNPATH_SEARCH_PATHS'] = ['$(inherited)', '@executable_path/Frameworks']
  settings['ASSETCATALOG_COMPILER_APPICON_NAME'] = ''
  settings['ASSETCATALOG_COMPILER_GENERATE_SWIFT_ASSET_SYMBOL_EXTENSIONS'] = 'NO'
  settings['PLATFORM_API_BASE_URL'] = default_backend_url
  settings['DEVELOPMENT_TEAM'] = development_team if development_team && !development_team.empty?
end

source_group = project.main_group.new_group('NexusPlatform', 'NexusPlatform')
sources_root_group = source_group.new_group('Sources', 'Sources')
app_sources_group = sources_root_group.new_group('NexusPlatform', 'NexusPlatform')
add_swift_sources(app_sources_group, SOURCE_ROOT, target)

info_group = project.main_group.new_group('NexusPlatformApp', 'NexusPlatformApp')
info_group.new_file('Info.plist')

add_package_dependency(
  project,
  target,
  url: 'https://github.com/Alamofire/Alamofire.git',
  version: '5.8.0',
  product_name: 'Alamofire'
)
add_package_dependency(
  project,
  target,
  url: 'https://github.com/SwiftyJSON/SwiftyJSON.git',
  version: '5.0.0',
  product_name: 'SwiftyJSON'
)
add_package_dependency(
  project,
  target,
  url: 'https://github.com/weichsel/ZIPFoundation.git',
  version: '0.9.19',
  product_name: 'ZIPFoundation'
)

scheme = Xcodeproj::XCScheme.new
scheme.configure_with_targets(target, nil, launch_target: true)
env_vars = scheme.launch_action.environment_variables
env_vars['PLATFORM_API_BASE_URL'] = default_backend_url
scheme.launch_action.environment_variables = env_vars
scheme.save_as(PROJECT_PATH.to_s, PROJECT_NAME, true)

project.save

puts "Generated #{PROJECT_PATH}"
puts "Bundle ID: #{product_bundle_identifier}"
puts "Backend URL: #{default_backend_url}"
