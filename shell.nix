{ pkgs ? import <nixpkgs> { } }:

pkgs.mkShell {
  packages = [ pkgs.jdk21 pkgs.maven ];
  JAVA_HOME = pkgs.jdk21.home;
}
