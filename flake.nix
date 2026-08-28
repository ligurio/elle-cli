{
  description = "Command-line frontend to transactional consistency checkers for black-box databases";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs = { self, nixpkgs }:
    let
      systems = [ "x86_64-linux" "aarch64-linux" ];
      forAllSystems = nixpkgs.lib.genAttrs systems;
      nixpkgsFor = system: import nixpkgs { inherit system; };

      version = "0.1.11";

      source = pkgs: pkgs.fetchFromGitHub {
        owner = "ligurio";
        repo = "elle-cli";
        rev = version;
        sha256 = "sha256-YqBk1ELWzkVckV97GNnuYMalfKLL0FhGOLQ/+ujPNs8=";
      };

      # Fetch all Maven/Clojars dependencies once. This is a fixed-output
      # derivation, so the sandbox allows `lein deps` to reach the network.
      mavenRepo = pkgs: pkgs.stdenv.mkDerivation {
        pname = "elle-cli-maven-repo";
        inherit version;
        src = source pkgs;

        nativeBuildInputs = with pkgs; [ leiningen jdk21 ];

        outputHashMode = "recursive";
        outputHashAlgo = "sha256";
        outputHash = "sha256-CalXuQ1AcVJiMk6O2LxPjlxsAnk8fut9P8FkLZ30dXs=";

        dontConfigure = true;
        dontFixup = true;

        buildPhase = ''
          runHook preBuild
          export HOME="$NIX_BUILD_TOP"
          export LEIN_JVM_OPTS="-Duser.home=$NIX_BUILD_TOP/m2home"
          make deps
          runHook postBuild
        '';

        installPhase = ''
          runHook preInstall
          mkdir -p "$out/repository"
          cp -r "$NIX_BUILD_TOP/m2home/.m2/repository"/. "$out/repository/"
          # Drop resolution metadata to keep the output deterministic.
          find "$out" -name '*.lastUpdated' -delete
          find "$out" -name '_remote.repositories' -delete
          runHook postInstall
        '';
      };

      elle-cli = pkgs: pkgs.stdenv.mkDerivation rec {
        pname = "elle-cli";
        inherit version;
        src = source pkgs;

        nativeBuildInputs = with pkgs; [
          leiningen
          jdk21
          makeWrapper
        ];

        buildPhase = ''
          runHook preBuild
          export HOME="$TMPDIR"
          export LEIN_JVM_OPTS="-Duser.home=$TMPDIR/m2home"
          mkdir -p "$TMPDIR/m2home/.m2"
          cp -r "${mavenRepo pkgs}/repository" "$TMPDIR/m2home/.m2/repository"
          make build LEIN="lein -o"
          runHook postBuild
        '';

        installPhase = ''
          runHook preInstall

          install -Dm644 target/${pname}-${version}-standalone.jar $out/share/java/${pname}-${version}-standalone.jar

          makeWrapper ${pkgs.jdk21}/bin/java $out/bin/${pname} \
            --add-flags "-Xmx32g -Djava.awt.headless=true" \
            --add-flags "-jar $out/share/java/${pname}-${version}-standalone.jar" \
            --prefix PATH : ${pkgs.lib.makeBinPath [ pkgs.graphviz ]}

          runHook postInstall
        '';

        meta = with pkgs.lib; {
          description = "Command-line frontend to transactional consistency checkers for black-box databases";
          homepage = "https://github.com/ligurio/elle-cli";
          license = licenses.epl20;
          mainProgram = "elle-cli";
          platforms = platforms.unix;
        };
      };
    in {
      packages = forAllSystems (system: {
        elle-cli = elle-cli (nixpkgsFor system);
        default = self.packages.${system}.elle-cli;
      });

      devShells = forAllSystems (system:
        let pkgs = nixpkgsFor system; in {
          default = pkgs.mkShell {
            name = "elle-cli-dev";
            buildInputs = with pkgs; [
              bash
              clojure
              leiningen
            ];
          };
        });
    };
}
