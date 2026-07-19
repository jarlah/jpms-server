{ pkgs ? import <nixpkgs> { } }:

pkgs.mkShell {
  packages = [ pkgs.jdk25 pkgs.maven ];
  JAVA_HOME = pkgs.jdk25.home;
}
