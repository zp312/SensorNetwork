function [successful] = convert()
%CONVERT Summary of this function goes here
%   Detailed explanation goes here
    for i = 222:322
        fileName = 'test%d-sample.mat';
        fileNo = i;
        fileName = sprintf(fileName,fileNo);
        load(fileName);
        convertedName = 'converted/test%d-sample-0a.txt';
        convertedName = sprintf(convertedName,fileNo);
        save( convertedName, 'X0', '-ASCII','-append');
        convertedName = 'converted/test%d-sample-0b.txt';
        convertedName = sprintf(convertedName,fileNo);
        save( convertedName, 'X1', '-ASCII','-append');
        convertedName = 'converted/test%d-sample-1a.txt';
        convertedName = sprintf(convertedName,fileNo);
        save( convertedName, 'X2', '-ASCII','-append');
        convertedName = 'converted/test%d-sample-1b.txt';
        convertedName = sprintf(convertedName,fileNo);
        save( convertedName, 'X3', '-ASCII','-append');
        convertedName = 'converted/test%d-sample-2a.txt';
        convertedName = sprintf(convertedName,fileNo);
        save( convertedName, 'X4', '-ASCII','-append');
        convertedName = 'converted/test%d-sample-2b.txt';
        convertedName = sprintf(convertedName,fileNo);
        save( convertedName, 'X5', '-ASCII','-append');
        convertedName = 'converted/test%d-sample-3a.txt';
        convertedName = sprintf(convertedName,fileNo);
        save( convertedName, 'X6', '-ASCII','-append');
        convertedName = 'converted/test%d-sample-3b.txt';
        convertedName = sprintf(convertedName,fileNo);
        save( convertedName, 'X7', '-ASCII','-append');
        convertedName = 'converted/test%d-sample-4a.txt';
        convertedName = sprintf(convertedName,fileNo);
        save( convertedName, 'X8', '-ASCII','-append');
        convertedName = 'converted/test%d-sample-4b.txt';
        convertedName = sprintf(convertedName,fileNo);
        save( convertedName, 'X9', '-ASCII','-append');
    end
end

