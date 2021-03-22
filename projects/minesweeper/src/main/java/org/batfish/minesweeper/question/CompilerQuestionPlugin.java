package org.batfish.minesweeper.question;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.StringAnswerElement;
import org.batfish.datamodel.questions.Question;
import org.batfish.minesweeper.nv.CompilerOptions;
import org.batfish.minesweeper.nv.CompilerOptions.NVFlags;
import org.batfish.minesweeper.nv.CompilerResult;
import org.batfish.minesweeper.nv.NVCompiler;
import org.batfish.minesweeper.utils.Tuple;
import org.batfish.question.QuestionPlugin;

@AutoService(Plugin.class)
public class CompilerQuestionPlugin extends QuestionPlugin {

  public static class CompileAnswerer extends Answerer {

    public CompileAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public AnswerElement answer() {
      CompileQuestion q = (CompileQuestion) _question;
      CompilerOptions flags = new CompilerOptions();
      if (((CompileQuestion) _question).getNextHop()) {
        flags.setFlag(NVFlags.nexthop);
      }
      if (((CompileQuestion) _question).getOrigin()) {
        flags.setFlag(NVFlags.origin);
      }
      if (((CompileQuestion) _question).getData()) {
        flags.setFlag(NVFlags.dataplane);
      }
      if (((CompileQuestion) _question).getNodeFaults()) {
        flags.setFlag(NVFlags.nodeFaults);
      }
      if (((CompileQuestion) _question).getASPath()) {
        flags.setFlag(NVFlags.asPath);
      }
      if (((CompileQuestion) _question).getMultiPath()) {
        flags.setFlag(NVFlags.multiPath);
      }
      if (((CompileQuestion) _question).getBoundedFaults() != 0) {
        flags.setFlag(NVFlags.boundedLinkFaults);
        flags.setLinkFaultsBound(((CompileQuestion) _question).getBoundedFaults());
      }

      NVCompiler c = new NVCompiler(_batfish, q.getFile(), flags);
      CompilerResult f = c.compile(q.getSinglePrefix());
      String cpComment = "(* models bgp, ospf, static routes" +
                        (flags.doNextHop() ? ", nexthop" : "") +
                        (flags.doASPath() ? ", AS Path as set" : "") +
                        (flags.doMultiPath() ? ", multipath" : "") +
                      (flags.doOrigin() ? ", route-origin" : "") + " *)\n\n";
      writeFile(q.getFile() + "_control.nv", f.getControlPlane(), cpComment);
      if (flags.doDataplane()) {
        writeFile(q.getFile() + "_data.nv", f.getDataPlane(), "");
      }
      if (flags.doNodeFaults()) {
        Tuple<String, Map<Prefix, String>> allNodeFaults = f.getAllNodeFaults();
        Tuple<String, Map<Prefix, String>> allLinkFaults = f.getAllLinkFaults();
        writeFile(q.getFile() + "_nodeFaults.nv", allNodeFaults.getFirst(), "");
        writeFile(q.getFile() + "_linkFaults.nv", allLinkFaults.getFirst(), "");

        for (Entry<Prefix, String> e : allNodeFaults.getSecond().entrySet()) {
          Ip ip = e.getKey().getStartIp();
          int pre = e.getKey().getPrefixLength();
          writeFile(q.getFile() + "_" + ip.toString() + "_" + pre + "_nodeFaults.nv", e.getValue(), "");
        }

        for (Entry<Prefix, String> e : allLinkFaults.getSecond().entrySet()) {
          Ip ip = e.getKey().getStartIp();
          int pre = e.getKey().getPrefixLength();
          writeFile(q.getFile() + "_" + ip.toString() + "_" + pre + "_linkFaults.nv", e.getValue(), "");
        }

      }
      if (flags.doBoundedLinkFaults()) {
        Tuple<String, Map<Prefix, String>> boundedLinkFaults = f.getBoundedLinkFaults();
        writeFile(q.getFile() + "_" + flags.getLinkFaultsBound() + "_linkFaults.nv", boundedLinkFaults.getFirst(), "(* Bounded link faults *)\n\n");

        for (Entry<Prefix, String> e : boundedLinkFaults.getSecond().entrySet()) {
          Ip ip = e.getKey().getStartIp();
          int pre = e.getKey().getPrefixLength();
          writeFile(q.getFile() + "_" + ip.toString() + "_" + pre + "_" + flags.getLinkFaultsBound() + "_linkFaults.nv", e.getValue(), "(*Bounded link faults for prefix: " + e.getKey().toString() +" *)\n\n");
        }
      }

      return new StringAnswerElement();
    }

    private void writeFile(String name, String contents, String comments) {
      try {
        File file = new File(name);
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(comments);
        fileWriter.write(contents);
        fileWriter.flush();
        fileWriter.close();
      } catch (IOException e) {
        System.out.println("An error occurred: could not write file.");
        e.printStackTrace();
      }
    }
  }

  public static class CompileQuestion extends HeaderQuestion {

    private static final String PROP_NEXTHOP = "doNextHop";
    private static final String PROP_ORIGIN = "doOrigin";
    private static final String PROP_DATA = "doData";
    private static final String PROP_DO_NODE_FAULTS = "doNodeFaults";
    private static final String PROP_DO_AS_PATH = "doASPath";
    private static final String PROP_DO_BOUNDED_FAULTS = "doBoundedLinkFaults";

    private static final String PROP_DO_MULTI_PATH = "doMultiPath";

    private static final String PROP_FILE = "file";

    private boolean _nextHop = false;

    private boolean _origin = false;

    private boolean _doNodeFaults = false;

    private boolean _data = false;

    private boolean _doASPath  = false;

    private boolean _doMultiPath = false;

    private int _doBoundedLinkFaults = 0;

    private String _file = "";

    @JsonProperty(PROP_NEXTHOP)
    public boolean getNextHop() {
      return _nextHop;
    }

    @JsonProperty(PROP_ORIGIN)
    public boolean getOrigin() {
      return _origin;
    }

    @JsonProperty(PROP_DO_NODE_FAULTS)
    public boolean getNodeFaults() {
      return _doNodeFaults;
    }

    @JsonProperty(PROP_DO_BOUNDED_FAULTS)
    public int getBoundedFaults() {
      return _doBoundedLinkFaults;
    }

    @JsonProperty(PROP_DO_BOUNDED_FAULTS)
    public void setBoundedFaults(int x) {
       this._doBoundedLinkFaults = x;
    }

    @JsonProperty(PROP_DO_AS_PATH)
    public boolean getASPath() { return _doASPath; }

    @JsonProperty(PROP_DO_MULTI_PATH)
    public boolean getMultiPath() {
      return _doMultiPath;
    }

    @JsonProperty(PROP_DATA)
    public boolean getData() { return _data; }

    @JsonProperty(PROP_FILE)
    public String getFile() {
      return _file;
    }

    @JsonProperty(PROP_NEXTHOP)
    public void setNextHop(boolean x) {
      this._nextHop = x;
    }

    @JsonProperty(PROP_ORIGIN)
    public void setOrigin(boolean x) {
      this._origin = x;
    }

    @JsonProperty(PROP_DATA)
    public void setData(boolean x) { this._data = x; }

    @JsonProperty(PROP_DO_NODE_FAULTS)
    public void setNodeFaults(boolean x) { this._doNodeFaults = x; }

    @JsonProperty(PROP_DO_AS_PATH)
    public void setASPath(boolean x) { this._doASPath = x; }

    @JsonProperty(PROP_DO_MULTI_PATH)
    public void setMultiPath(boolean x) {
      this._doMultiPath = x;
    }

    @JsonProperty(PROP_FILE)
    public void setFile(String x) {
      this._file = x;
    }

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @Override
    public String getName() {
      return "compile";
    }
  }

  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new CompileAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new CompileQuestion();
  }
}
