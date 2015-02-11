% Example 1. Rigid CPD point-set registration. No options are set, so the
% default ones are used. 2D fish point-set.
clear all; close all; clc;

directory_name = uigetdir;
files = dir(directory_name);

fileIndex = find(~[files.isdir]);

for i = 1:length(fileIndex)

    fileName = files(fileIndex(i)).name;
    load(fileName);% Y=X;

    X1 = 0.5 * X1;
    % Initial point-sets
    figure,cpd_plot_iter(X0, X1); title('Before');

    % Add a random rotation and scaling
    R=cpd_R(rand(1));
    s=rand(1);
    X0=s*X0*R';


    Transform=cpd_register(X0,X1);


    % Registered point-sets
    figure,cpd_plot_iter(X0, Transform.Y);  title('After');

    % Rotation and scaling errors after the registration
    E_R=norm(R-Transform.R)
    E_s=norm(s-Transform.s)
end