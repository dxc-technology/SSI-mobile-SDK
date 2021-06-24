./gradlew :"BuildDevice" -PXCODE_CONFIGURATION=Release
./gradlew :"BuildSimulator" -PXCODE_CONFIGURATION=Release

lipo build/xcode-framework-arm/ssi_agent.framework/ssi_agent -extract arm64 -output build/xcode-framework-arm/ssi_agent.framework/ssi_agent
lipo -info build/xcode-framework-arm/ssi_agent.framework/ssi_agent
lipo build/xcode-framework-X64/ssi_agent.framework/ssi_agent -extract x86_64 -output build/xcode-framework-X64/ssi_agent.framework/ssi_agent
lipo -info build/xcode-framework-X64/ssi_agent.framework/ssi_agent

FRAMEWORK_NAME="ssi_agent"
ARM64PATH="build/xcode-framework-arm/${FRAMEWORK_NAME}.framework"
X64PATH="build/xcode-framework-X64/${FRAMEWORK_NAME}.framework"
UNIVERSAL_PATH="build/xcode-framework-universal/"
xcodebuild -create-xcframework -framework "${ARM64PATH}" -framework "${X64PATH}" -output "${UNIVERSAL_PATH}/${FRAMEWORK_NAME}.xcframework"