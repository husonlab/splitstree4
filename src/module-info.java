module splitstree {
    requires transitive jloda;
    requires transitive com.install4j.runtime;
    requires transitive java.xml;
    requires transitive java.desktop;

    requires Jama;

    exports splitstree4.algorithms;
    exports splitstree4.algorithms.additional;
    exports splitstree4.algorithms.characters;
    exports splitstree4.algorithms.distances;
    exports splitstree4.algorithms.quartets;
    exports splitstree4.algorithms.reticulate;
    exports splitstree4.algorithms.splits;
    exports splitstree4.algorithms.splits.reticulate;
    exports splitstree4.algorithms.splits.reticulateTree;
    exports splitstree4.algorithms.trees;
    exports splitstree4.algorithms.unaligned;
    exports splitstree4.algorithms.util;
    exports splitstree4.algorithms.util.optimization;
    exports splitstree4.algorithms.util.simulate;
    exports splitstree4.algorithms.util.simulate.RandomVariables;
    exports splitstree4.analysis;
    exports splitstree4.analysis.bootstrap;
    exports splitstree4.analysis.characters;
    exports splitstree4.analysis.distances;
    exports splitstree4.analysis.network;
    exports splitstree4.analysis.quartets;
    exports splitstree4.analysis.splits;
    exports splitstree4.analysis.trees;
    exports splitstree4.analysis.unaligned;
    exports splitstree4.core;
    exports splitstree4.externalIO.exports;
    exports splitstree4.externalIO.imports;
    exports splitstree4.gui;
	exports splitstree4.gui.treepainter;
	exports splitstree4.gui.algorithms;
    exports splitstree4.gui.algorithms.filter;
    exports splitstree4.gui.algorithms.modify;
    exports splitstree4.gui.algorithms.select;
    exports splitstree4.gui.analysis;
    exports splitstree4.gui.bootstrap;
    exports splitstree4.gui.confidence;
    exports splitstree4.gui.formatter;
    exports splitstree4.gui.input;
    exports splitstree4.gui.main;
    exports splitstree4.gui.nodeEdge;
    exports splitstree4.gui.preferences;
    exports splitstree4.gui.search;
    exports splitstree4.gui.spreadsheet;
    exports splitstree4.gui.undo;
    exports splitstree4.main;
    exports splitstree4.models;
    exports splitstree4.nexus;
    exports splitstree4.progs;
    exports splitstree4.resources.icons;
    exports splitstree4.resources.images;
    exports splitstree4.util;
    exports splitstree4.util.matrix;

    exports splitstree4.resources;
    opens splitstree4.resources.icons;
    opens splitstree4.resources.images;

}
