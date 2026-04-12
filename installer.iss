#define AppName "RSPS Hub"
#define AppVersion "1.0.0"
#define AppPublisher "RSPS Hub"
#define AppURL "https://therspshub.com"
#define AppExeName "RSPS Hub.exe"
#define AppDir "build\installer\RSPS Hub"

[Setup]
AppId={{A3F2C1D4-8E7B-4F9A-B2C3-D4E5F6A7B8C9}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
AppPublisherURL={#AppURL}
AppSupportURL={#AppURL}
AppUpdatesURL={#AppURL}
DefaultDirName={localappdata}\RSPSHub
DefaultGroupName={#AppName}
DisableProgramGroupPage=yes
OutputDir=build\dist
OutputBaseFilename=RSPSHub-Setup-{#AppVersion}
SetupIconFile=src\app_icon.ico
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=lowest
UninstallDisplayIcon={app}\{#AppExeName}
UninstallDisplayName={#AppName}
DisableWelcomePage=yes
CloseApplications=no
RestartApplications=no
CreateUninstallRegKey=yes

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
Source: "{#AppDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "src\app_icon.ico"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{autoprograms}\{#AppName}"; Filename: "{app}\{#AppExeName}"; IconFilename: "{app}\app_icon.ico"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExeName}"; IconFilename: "{app}\app_icon.ico"
