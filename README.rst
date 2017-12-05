Java ASM-based Dataflow Analysis Tool
#####################################

This tool uses the `ASM Frameworks <http://asm.ow2.org/>`_ data flow analysis
capabilities to extract a control- and data-flow graph for a compiled Java
program.

The Context
===========

This tool is part of the prettycat suite. It produces XML output which can be
used with the
`Prettycat Graph Analysis Tool <https://github.com/martin-beck/prettycat-graph-analysis>`_
to further process the resulting graphs. See the linked readme for more details.

Dataflow Analysis
=================

Much of the work is done by the data flow analysis provided by the ASM
framework. This tool mainly does the following:

* resolve classes to their corresponding .class files (possibly inside .jar
  files) as found in the class path given as command line options.
* analyse all methods of the classes given on the command line and extract their
  control- and data-flow graph.
* for all method calls inside those methods, ensure that the called methods are
  also analysed (given that their classes are inside the ClI classpath)
* write a single XML file which contains the graphs for all methods

This XML file can be used with the Prettycat Graph Analysis Tool for plotting,
inlining and other useful operations.
