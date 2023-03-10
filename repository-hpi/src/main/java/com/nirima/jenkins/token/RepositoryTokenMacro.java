/*
 * The MIT License
 *
 * Copyright (c) 2011, Nigel Magnay / NiRiMa
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.nirima.jenkins.token;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.ParametersAction;
import hudson.model.RunParameterValue;
import hudson.model.TaskListener;

import jenkins.model.Jenkins;

import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

import java.io.IOException;

@Extension
public class RepositoryTokenMacro extends DataBoundTokenMacro {

    @Parameter(required = false)
    boolean chain;

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals("REPOSITORY_UPSTREAM");
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> abstractBuild, TaskListener taskListener, String s) throws MacroEvaluationException, IOException, InterruptedException {
        try {
            String theProject = null;
            String theBuild = null;

            Cause.UpstreamCause theCause = abstractBuild.getCause(Cause.UpstreamCause.class);

            if (theCause == null) {
                ParametersAction action = abstractBuild.getAction(ParametersAction.class);
                RunParameterValue value = (RunParameterValue) action.getParameter("Upstream");

                theProject = value.getJobName();
                theBuild   = value.getNumber();
            } else {
                theProject = theCause.getUpstreamProject();
                theBuild = "" + theCause.getUpstreamBuild();
            }

            String root = Jenkins.get().getRootUrl() + "plugin/repository/project/" + theProject + "/Build/"
                    + theBuild;

            if (chain)
                return root + "/repositoryChain/";
            else
                return root + "/repository/";
        } catch (Exception ex) {
            // Unknown..
            return "";
        }

    }
}
