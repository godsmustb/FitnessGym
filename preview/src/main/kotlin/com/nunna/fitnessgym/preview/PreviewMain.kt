package com.nunna.fitnessgym.preview

import com.nunna.fitnessgym.anim.Activation
import com.nunna.fitnessgym.anim.Camera
import com.nunna.fitnessgym.anim.Exercise
import com.nunna.fitnessgym.anim.FigureScene
import com.nunna.fitnessgym.anim.Library
import com.nunna.fitnessgym.anim.Motion
import com.nunna.fitnessgym.anim.Palette
import com.nunna.fitnessgym.anim.Prim
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Renders contact sheets to preview-out/<id>.png: 6 phases across, 2 camera views down
 * (the template's default view and a side view). Use it to check a motion before shipping.
 *
 *   gradlew :preview:run --args="squat lunge"     templates (demo muscles from the first exercise using it)
 *   gradlew :preview:run --args="ex:dumbbell-bench-press"   one exercise
 *   gradlew :preview:run --args="all"             every template
 */
fun main(args: Array<String>) {
    val content = File("content")
    val motions = File(content, "motions").listFiles { f -> f.extension == "json" }!!.associate { f ->
        val m = try { Motion.parse(f.readText()) } catch (e: Exception) { System.err.println("BAD ${f.name}: ${e.message}"); null }
        f.nameWithoutExtension to m
    }
    val exFile = File(content, "exercises.json")
    val exercises = if (exFile.exists()) Library.parseExercises(exFile.readText()) else emptyList()
    val outDir = File("preview-out").apply { mkdirs() }

    val targets = if (args.isEmpty() || args[0] == "all") motions.keys.sorted() else args.toList()
    for (raw in targets) {
        val big = raw.startsWith("big:")
        // Optional demo muscles: squat@quadriceps,glutes/hamstrings  (primary/secondary)
        val t = raw.removePrefix("big:").substringBefore("@")
        val demo = raw.substringAfter("@", "").takeIf { it.isNotEmpty() }?.let { d ->
            val (pri, sec) = (d.split("/") + "").take(2)
            Activation(pri.split(",").filter { it.isNotBlank() }.toSet(), sec.split(",").filter { it.isNotBlank() }.toSet())
        }
        val ex: Exercise? = if (t.startsWith("ex:")) exercises.firstOrNull { it.id == t.removePrefix("ex:") } else exercises.firstOrNull { it.motion == t }
        val motionId = ex?.motion ?: t
        val motion = motions[motionId]
        if (motion == null) { System.err.println("No motion '$motionId'"); continue }
        val act = demo ?: ex?.activation() ?: Activation(emptySet(), emptySet())
        val file = File(outDir, "${t.removePrefix("ex:")}.png")
        render(motion, act, "${motion.template.id}${ex?.let { " - " + it.name } ?: ""}", file, big)
        println("wrote ${file.path}")
    }
}

fun render(motion: Motion, act: Activation, title: String, file: File, big: Boolean = false) {
    val cw = if (big) 560 else 260; val ch = if (big) 640 else 300; val cols = if (big) 3 else 6
    val scene = FigureScene(motion, act)
    val views = listOf(scene.defaultCamera(), Camera(-90.0, 5.0))
    val img = BufferedImage(cw * cols, ch * views.size + 28, BufferedImage.TYPE_INT_ARGB)
    val g = img.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.color = Color(Palette.BG, true); g.fillRect(0, 0, img.width, img.height)
    g.color = Color.WHITE; g.font = Font("SansSerif", Font.BOLD, 16); g.drawString(title, 8, 20)
    for ((r, cam) in views.withIndex()) for (c in 0 until cols) {
        val phase = c / cols.toDouble()
        val prims = scene.frame(phase, cw.toFloat(), ch.toFloat(), cam)
        val g2 = g.create(c * cw, 28 + r * ch, cw, ch) as java.awt.Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        for (p in prims) draw(g2, p)
        g2.color = Color(0xFF8899AA.toInt()); g2.font = Font("SansSerif", Font.PLAIN, 11)
        g2.drawString("t=%.2f  yaw %.0f".format(phase, cam.yaw), 6, ch - 6)
        g2.dispose()
    }
    g.dispose()
    ImageIO.write(img, "png", file)
}

private fun draw(g: java.awt.Graphics2D, p: Prim) {
    if (p.xy.size < 4) return
    val path = Path2D.Float()
    path.moveTo(p.xy[0], p.xy[1])
    var i = 2
    while (i < p.xy.size) { path.lineTo(p.xy[i], p.xy[i + 1]); i += 2 }
    if (p.closed) path.closePath()
    if (p.fill != 0 && p.closed) { g.color = Color(p.fill, true); g.fill(path) }
    if (p.stroke != 0 && p.strokeW > 0) {
        g.color = Color(p.stroke, true)
        g.stroke = BasicStroke(p.strokeW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.draw(path)
    }
}
