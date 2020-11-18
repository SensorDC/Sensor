package org.sensor.conflict;

import org.sensor.conflict.util.Conf;
import org.sensor.conflict.writer.SemanticsConflictSupImplWriter;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "SemanticsConflictSupImpl", defaultPhase = LifecyclePhase.VALIDATE)
public class SemanticsConflictSupImplMojo extends ConflictMojo {
    @Override
    public void run() {
        new SemanticsConflictSupImplWriter().writeSemanticsPath(Conf.outDir);
    }
}
