package com.claudeapp.snake;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new GameView(this));
    }

    static class Obj {
        int type, w, h, col, col2, seed;
        float x, y;
        int[] look;
    }

    static class GameView extends View {
        static final int MENU = 0, CUSTOM = 1, PLAY = 2, WIN = 3;
        static final int T_TREE = 0, T_BUSH = 1, T_FLOWER = 2, T_CAT = 3, T_LAMP = 4, T_FENCE = 5;
        static final int CREAM = Color.rgb(255, 243, 224);
        static final int BROWN = Color.rgb(93, 64, 55);
        static final int BERRY = Color.rgb(230, 70, 90);
        static final int[] GWS = {64, 96, 128};
        static final int[] FUR = {Color.rgb(230, 150, 70), Color.rgb(150, 150, 165), Color.rgb(55, 55, 65),
                Color.rgb(245, 235, 220), Color.rgb(130, 90, 60), Color.rgb(240, 175, 195)};
        static final int[] EYE = {Color.rgb(70, 200, 110), Color.rgb(80, 150, 240),
                Color.rgb(240, 210, 60), Color.rgb(175, 100, 235)};
        static final String[] PAT = {"Uni", "Rayures", "Taches", "Smoking"};
        static final String[] ACC = {"Aucun", "Noeud", "Bonnet", "Écharpe"};
        static final int[] WALL = {Color.rgb(255, 236, 200), Color.rgb(255, 205, 210), Color.rgb(200, 230, 255),
                Color.rgb(220, 255, 210), Color.rgb(255, 245, 160)};
        static final int[] ROOF = {Color.rgb(183, 80, 70), Color.rgb(90, 110, 170),
                Color.rgb(150, 100, 80), Color.rgb(70, 130, 110)};
        static final int[][] TREE_COL = {
                {Color.rgb(70, 170, 80), Color.rgb(95, 190, 95), Color.rgb(255, 175, 195)},
                {Color.rgb(120, 155, 70), Color.rgb(215, 135, 60), Color.rgb(255, 160, 150)},
                {Color.rgb(30, 90, 70), Color.rgb(40, 80, 105), Color.rgb(95, 75, 125)}};
        static final int[][] BUSH_COL = {
                {Color.rgb(60, 160, 75), Color.rgb(85, 185, 95)},
                {Color.rgb(100, 140, 65), Color.rgb(150, 140, 60)},
                {Color.rgb(28, 80, 62), Color.rgb(38, 95, 75)}};
        static final int[] FLOWER_COL = {Color.rgb(255, 235, 90), Color.rgb(255, 255, 255),
                Color.rgb(255, 140, 170), Color.rgb(170, 170, 255)};
        static final int[] BF = {Color.rgb(255, 160, 190), Color.rgb(255, 220, 100), Color.rgb(160, 200, 255)};

        SharedPreferences prefs;
        Random rnd = new Random();
        Paint p = new Paint();
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable ticker = new Runnable() {
            @Override
            public void run() {
                invalidate();
                handler.postDelayed(this, 80);
            }
        };

        int state = MENU, diff = 1, GW = 96, GH = 100, groundY = 50, theme = 0, topInset = 0;
        float ps = 0, hillSeed = 0;
        int skyTop, skyBot, groundCol, groundDark, hillCol;
        ArrayList<Obj> objs = new ArrayList<>();
        ArrayList<float[]> clouds = new ArrayList<>();
        ArrayList<int[]> stars = new ArrayList<>();
        ArrayList<int[]> tufts = new ArrayList<>();
        ArrayList<int[]> houses = new ArrayList<>();
        ArrayList<float[]> flyers = new ArrayList<>();
        ArrayList<float[]> marks = new ArrayList<>();
        ArrayList<RectF> hits = new ArrayList<>();
        ArrayList<Integer> hitIds = new ArrayList<>();
        int[] look = {0, 1, 0, 0};
        Obj target;
        long startT, foundT, hintUntil, bestMs;
        boolean newBest;
        float hintX, hintY;
        Canvas cv;
        float cx0, cy0, ck;

        GameView(Context c) {
            super(c);
            prefs = c.getSharedPreferences("chat", Context.MODE_PRIVATE);
            int[] def = {0, 1, 0, 0};
            int[] max = {FUR.length, EYE.length, PAT.length, ACC.length};
            for (int i = 0; i < 4; i++) {
                int v = prefs.getInt("look" + i, def[i]);
                look[i] = (v >= 0 && v < max[i]) ? v : def[i];
            }
            tp.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        }

        // ---------- cycle de vie ----------
        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            handler.post(ticker);
        }

        @Override
        protected void onDetachedFromWindow() {
            handler.removeCallbacks(ticker);
            super.onDetachedFromWindow();
        }

        @Override
        public WindowInsets onApplyWindowInsets(WindowInsets insets) {
            topInset = insets.getSystemWindowInsetTop();
            return super.onApplyWindowInsets(insets);
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            showMenu();
        }

        void saveLook() {
            SharedPreferences.Editor e = prefs.edit();
            for (int i = 0; i < 4; i++) e.putInt("look" + i, look[i]);
            e.apply();
        }

        void showMenu() {
            genScene(1, false);
            state = MENU;
        }

        void startGame(int d) {
            genScene(d, true);
            state = PLAY;
            startT = SystemClock.uptimeMillis();
            hintUntil = 0;
        }

        // ---------- génération du décor ----------
        Obj obj(int type, float x, float y) {
            Obj o = new Obj();
            o.type = type;
            o.x = x;
            o.y = y;
            o.seed = rnd.nextInt(1000);
            objs.add(o);
            return o;
        }

        void genScene(int d, boolean cats) {
            diff = d;
            GW = GWS[d];
            ps = (float) getWidth() / GW;
            GH = (int) (getHeight() / ps) + 1;
            groundY = (int) (GH * 0.5f);
            hillSeed = rnd.nextFloat() * 10f;
            theme = rnd.nextInt(3);
            if (theme == 0) {
                skyTop = Color.rgb(120, 195, 240); skyBot = Color.rgb(205, 236, 250);
                groundCol = Color.rgb(125, 200, 95); groundDark = Color.rgb(100, 175, 80);
                hillCol = Color.rgb(150, 215, 130);
            } else if (theme == 1) {
                skyTop = Color.rgb(240, 130, 110); skyBot = Color.rgb(255, 215, 160);
                groundCol = Color.rgb(120, 170, 90); groundDark = Color.rgb(95, 145, 75);
                hillCol = Color.rgb(145, 150, 110);
            } else {
                skyTop = Color.rgb(18, 22, 56); skyBot = Color.rgb(70, 70, 130);
                groundCol = Color.rgb(45, 100, 80); groundDark = Color.rgb(35, 80, 65);
                hillCol = Color.rgb(40, 70, 90);
            }
            objs.clear(); clouds.clear(); stars.clear(); tufts.clear();
            houses.clear(); flyers.clear(); marks.clear();
            target = null;
            int ground = GH - groundY;

            for (int i = 0; i < GW / 20; i++)
                clouds.add(new float[]{rnd.nextFloat() * (GW + 20), 3 + rnd.nextInt(Math.max(1, groundY / 2)),
                        10 + rnd.nextInt(10), 1.5f + rnd.nextFloat() * 3f});
            if (theme == 2)
                for (int i = 0; i < GW / 2; i++)
                    stars.add(new int[]{rnd.nextInt(GW), rnd.nextInt(Math.max(1, groundY - 6)), rnd.nextInt(8)});
            for (int i = 0; i < GW * ground / 45; i++)
                tufts.add(new int[]{rnd.nextInt(GW), groundY + 1 + rnd.nextInt(Math.max(1, ground - 1)), rnd.nextInt(2)});

            int nh = d + 2, seg = GW / nh;
            for (int i = 0; i < nh; i++) {
                int hw = 14 + rnd.nextInt(8);
                houses.add(new int[]{i * seg + 1 + rnd.nextInt(Math.max(1, seg - hw - 1)),
                        groundY + 3 + rnd.nextInt(3), hw, 14 + rnd.nextInt(7),
                        WALL[rnd.nextInt(WALL.length)], ROOF[rnd.nextInt(ROOF.length)],
                        rnd.nextInt(2), rnd.nextInt(100)});
            }
            for (int i = 0; i < GW / 9; i++) {
                Obj o = obj(T_TREE, rnd.nextInt(GW), groundY + 8 + rnd.nextInt(Math.max(1, ground - 12)));
                o.w = 4 + rnd.nextInt(3);
                o.h = 5 + rnd.nextInt(4);
                o.col = TREE_COL[theme][rnd.nextInt(3)];
            }
            for (int i = 0; i < GW / 7; i++) {
                Obj o = obj(T_BUSH, rnd.nextInt(GW + 8) - 4, groundY + 6 + rnd.nextInt(Math.max(1, ground - 9)));
                o.w = 9 + rnd.nextInt(7);
                o.h = 4 + rnd.nextInt(3);
                o.col = BUSH_COL[theme][rnd.nextInt(2)];
                o.col2 = rnd.nextInt(3) == 0 ? BERRY : 0;
            }
            for (int i = 0; i < GW / 3; i++) {
                Obj o = obj(T_FLOWER, rnd.nextInt(GW - 3), groundY + 4 + rnd.nextInt(Math.max(1, ground - 5)));
                o.col = FLOWER_COL[rnd.nextInt(FLOWER_COL.length)];
            }
            for (int i = 0; i < 1 + d; i++)
                obj(T_LAMP, rnd.nextInt(GW), groundY + 10 + rnd.nextInt(Math.max(1, ground - 22)));
            for (int i = 0; i < d + 1; i++) {
                Obj o = obj(T_FENCE, rnd.nextInt(GW - 14), groundY + 8 + rnd.nextInt(Math.max(1, ground - 14)));
                o.w = 14;
            }
            for (int i = 0; i < GW / 10; i++)
                flyers.add(new float[]{rnd.nextFloat() * GW, groundY - 4 + rnd.nextFloat() * ground * 0.8f,
                        rnd.nextFloat() * 6.28f, 0.6f + rnd.nextFloat()});

            if (cats) {
                float[] pp = pickPos();
                final Obj tg = newCat(pp, look.clone());
                target = tg;
                objs.add(tg);
                // rien ne doit cacher le chat par hasard
                objs.removeIf(o -> o != tg && (o.type == T_TREE || o.type == T_BUSH || o.type == T_FENCE)
                        && o.y > tg.y && o.y - tg.y < 24
                        && (o.type == T_TREE ? o.x + o.w : o.x + o.w) > tg.x - 1
                        && (o.type == T_TREE ? o.x - o.w : o.x) < tg.x + 11);
                addCover(tg);
                int nd = new int[]{0, 5, 12}[d];
                for (int i = 0; i < nd; i++) {
                    Obj o = newCat(pickPos(), decoyLook());
                    objs.add(o);
                    addCover(o);
                }
            }
            Collections.sort(objs, (a, b) -> Float.compare(a.y, b.y));
        }

        float[] pickPos() {
            int yMin = Math.max(groundY + 12, (int) ((topInset + getWidth() * 0.16f) / ps) + 14);
            for (int tries = 0; tries < 60; tries++) {
                float x = 3 + rnd.nextInt(Math.max(1, GW - 17));
                float y = yMin + rnd.nextInt(Math.max(1, GH - 6 - yMin));
                boolean ok = true;
                for (Obj o : objs)
                    if (o.type == T_CAT && Math.abs(o.x - x) < 14 && Math.abs(o.y - y) < 8) { ok = false; break; }
                if (ok) return new float[]{x, y};
            }
            return new float[]{GW / 2f, GH * 0.75f};
        }

        Obj newCat(float[] pos, int[] lk) {
            Obj o = new Obj();
            o.type = T_CAT;
            o.x = pos[0];
            o.y = pos[1];
            o.look = lk;
            o.seed = rnd.nextInt(100);
            return o;
        }

        int[] decoyLook() {
            int[] l = new int[4];
            for (;;) {
                l[0] = rnd.nextInt(FUR.length);
                l[1] = rnd.nextInt(EYE.length);
                l[2] = rnd.nextInt(PAT.length);
                l[3] = rnd.nextInt(ACC.length);
                boolean same = l[0] == look[0] && l[1] == look[1] && l[2] == look[2] && l[3] == look[3];
                if (same) continue;
                if (diff < 2 && l[0] == look[0]) continue;
                return l;
            }
        }

        void addCover(Obj cat) {
            if (diff == 0) return;
            if (rnd.nextFloat() > (diff == 1 ? 0.6f : 0.9f)) return;
            Obj b = obj(T_BUSH, cat.x - 2 + rnd.nextInt(2), cat.y + 1);
            b.w = 14;
            b.h = diff == 1 ? 5 : 7;
            b.col = BUSH_COL[theme][rnd.nextInt(2)];
            b.col2 = rnd.nextInt(3) == 0 ? BERRY : 0;
        }

        // ---------- utilitaires de dessin ----------
        static int shade(int col, float f) {
            return Color.rgb(Math.min(255, (int) (Color.red(col) * f)),
                    Math.min(255, (int) (Color.green(col) * f)),
                    Math.min(255, (int) (Color.blue(col) * f)));
        }

        static int mix(int a, int b, float f) {
            return Color.rgb((int) (Color.red(a) + (Color.red(b) - Color.red(a)) * f),
                    (int) (Color.green(a) + (Color.green(b) - Color.green(a)) * f),
                    (int) (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * f));
        }

        void rect(Canvas c, float x, float y, float w, float h, int col) {
            p.setColor(col);
            c.drawRect(x, y, x + w, y + h, p);
        }

        void disc(Canvas c, float cx, float cy, int r, int col) {
            for (int dy = -r; dy <= r; dy++) {
                int hw = (int) Math.sqrt(r * r - dy * dy);
                rect(c, (int) cx - hw, (int) cy + dy, 2 * hw + 1, 1, col);
            }
        }

        void cell(int col, int row, int color) {
            p.setColor(color);
            cv.drawRect(cx0 + col * ck, cy0 + row * ck, cx0 + (col + 1) * ck, cy0 + (row + 1) * ck, p);
        }

        void drawCat(Canvas c, float x, float y, float k, int[] lk, long t, int seed) {
            cv = c; cx0 = x; cy0 = y; ck = k;
            int fur = FUR[lk[0]], eye = EYE[lk[1]], pat = lk[2], acc = lk[3];
            int dark = shade(fur, 0.7f);
            int white = Color.rgb(250, 245, 235), pink = Color.rgb(240, 150, 165);
            boolean blink = (t + seed * 131L) % 3300 < 200;
            boolean wag = ((t + seed * 57L) / 380) % 2 == 0;
            boolean twitch = (t + seed * 29L) % 5200 < 160;
            // queue
            cell(8, 8, fur); cell(9, 8, fur); cell(9, 7, pat == 1 ? dark : fur);
            if (wag) cell(9, 6, fur); else cell(9, 9, fur);
            // corps
            for (int r = 6; r <= 8; r++) for (int q = 2; q <= 7; q++) cell(q, r, fur);
            cell(2, 9, fur); cell(3, 9, fur); cell(6, 9, fur); cell(7, 9, fur);
            // tête et oreilles
            for (int r = 1; r <= 5; r++) for (int q = 1; q <= 8; q++) cell(q, r, fur);
            if (!twitch) cell(1, 0, fur);
            cell(8, 0, fur);
            // motifs
            if (pat == 1) {
                cell(3, 1, dark); cell(6, 1, dark);
                cell(3, 6, dark); cell(3, 7, dark); cell(6, 6, dark); cell(6, 7, dark);
            } else if (pat == 2) {
                cell(6, 1, dark); cell(7, 1, dark); cell(7, 2, dark);
                cell(3, 7, dark); cell(4, 7, dark); cell(6, 8, dark);
            } else if (pat == 3) {
                for (int q = 3; q <= 6; q++) { cell(q, 4, white); cell(q, 5, white); }
                for (int r = 6; r <= 8; r++) { cell(4, r, white); cell(5, r, white); }
                cell(2, 9, white); cell(3, 9, white); cell(6, 9, white); cell(7, 9, white);
            }
            // visage
            cell(4, 4, pink); cell(5, 4, pink);
            int ec = blink ? dark : eye;
            cell(3, 3, ec); cell(6, 3, ec);
            // accessoires
            if (acc == 1) {
                int red = Color.rgb(220, 60, 80), redD = Color.rgb(160, 30, 55);
                cell(3, 6, red); cell(6, 6, red); cell(4, 6, redD); cell(5, 6, redD);
            } else if (acc == 2) {
                int blue = Color.rgb(70, 130, 200);
                for (int q = 3; q <= 6; q++) cell(q, 0, blue);
                cell(4, -1, blue); cell(5, -1, blue); cell(4, -2, white);
            } else if (acc == 3) {
                int or = Color.rgb(235, 130, 40), ye = Color.rgb(255, 205, 80);
                for (int q = 2; q <= 7; q++) cell(q, 6, (q % 2 == 0) ? or : ye);
                cell(6, 7, or); cell(6, 8, or);
            }
        }

        // ---------- dessin du décor ----------
        void drawScene(Canvas c, long t) {
            int bands = 14;
            for (int i = 0; i < bands; i++) {
                int y0 = groundY * i / bands, y1 = groundY * (i + 1) / bands + 1;
                rect(c, 0, y0, GW, y1 - y0, mix(skyTop, skyBot, i / (float) (bands - 1)));
            }
            if (theme == 0) disc(c, GW * 0.22f, groundY * 0.28f, 6, Color.rgb(255, 240, 150));
            else if (theme == 1) disc(c, GW * 0.7f, groundY * 0.78f, 7, Color.rgb(255, 170, 90));
            else {
                disc(c, GW * 0.78f, groundY * 0.25f, 5, Color.rgb(250, 248, 225));
                for (int[] s : stars)
                    if (((t / 350) + s[2]) % 5 != 0) rect(c, s[0], s[1], 1, 1, Color.WHITE);
            }
            int cloudCol = theme == 2 ? Color.rgb(105, 105, 160) : theme == 1 ? Color.rgb(255, 228, 215) : Color.WHITE;
            for (float[] cl : clouds) {
                float x = ((cl[0] + t * cl[3] / 1000f) % (GW + cl[2])) - cl[2];
                rect(c, x + 3, cl[1], cl[2] - 6, 2, cloudCol);
                rect(c, x, cl[1] + 2, cl[2], 3, cloudCol);
                rect(c, x + 2, cl[1] + 1, cl[2] - 4, 1, cloudCol);
            }
            for (int x = 0; x < GW; x++) {
                int hy = groundY - 7 - (int) (4 * Math.sin(x * 0.11 + hillSeed) + 3 * Math.sin(x * 0.047 + hillSeed * 2));
                rect(c, x, hy, 1, groundY - hy + 1, hillCol);
            }
            rect(c, 0, groundY, GW, GH - groundY + 1, groundCol);
            for (int[] tf : tufts) rect(c, tf[0], tf[1], 1, tf[2] == 0 ? 2 : 1, groundDark);

            for (int[] hs : houses) {
                int x = hs[0], by = hs[1], w = hs[2], h = hs[3];
                int rh = w / 2 + 1, top = by - h - rh;
                rect(c, x, by - h, w, h, hs[4]);
                int sy0 = top + rh / 3 - 2;
                if (hs[6] == 1) {
                    int chx = x + w * 3 / 4;
                    rect(c, chx, sy0, 3, rh * 2 / 3 + 2, Color.rgb(150, 85, 75));
                    for (int k = 0; k < 4; k++) {
                        float ph = ((t / 260f) + k * 3.5f) % 14f;
                        float sx = chx + 1 + (float) Math.sin(ph * 0.6 + k) * 1.6f;
                        p.setColor(Color.argb((int) (210 - ph * 14), 240, 240, 240));
                        c.drawRect(sx, sy0 - ph, sx + 2, sy0 - ph + 2, p);
                    }
                }
                for (int r = 0; r < rh; r++) {
                    int rw = (int) ((w + 4) * (r + 1) / (float) rh);
                    rect(c, x - 2 + (w + 4 - rw) / 2, top + r, rw, 1, hs[5]);
                }
                int nx = (w - 2) / 6, ny = (h - 8) / 6;
                for (int i = 0; i < nx; i++) {
                    for (int j = 0; j < ny; j++) {
                        int wx = x + 2 + i * 6, wy = by - h + 3 + j * 6;
                        boolean lit = theme != 0 && ((hs[7] + i * 7 + j * 13 + (int) (t / 6000)) % 3 != 0);
                        int col;
                        if (lit) col = ((t / 170 + i + j + hs[7]) % 41 == 0) ? Color.rgb(255, 190, 90) : Color.rgb(255, 224, 130);
                        else col = theme == 0 ? Color.rgb(175, 218, 240) : Color.rgb(70, 80, 115);
                        rect(c, wx, wy, 4, 4, col);
                        rect(c, wx - 1, wy + 4, 6, 1, shade(hs[4], 0.8f));
                    }
                }
                rect(c, x + w / 2 - 1, by - 5, 3, 5, Color.rgb(120, 78, 56));
            }

            for (Obj o : objs) {
                int ix = (int) o.x, iy = (int) o.y;
                switch (o.type) {
                    case T_TREE: {
                        int R = o.w, th = o.h;
                        rect(c, ix - 1, iy - th, 3, th, Color.rgb(110, 80, 55));
                        int sway = (int) Math.round(Math.sin(t / 1100.0 + o.seed));
                        int cy = iy - th - R + 2;
                        for (int dy = -R; dy <= R; dy++) {
                            int hw = (int) Math.sqrt(R * R - dy * dy);
                            rect(c, ix - hw + sway, cy + dy, 2 * hw + 1, 1, dy < -R / 3 ? shade(o.col, 1.15f) : o.col);
                        }
                        for (int k = 0; k < 5; k++) {
                            int ax = (o.seed * (k + 3) * 7) % (2 * R - 1) - R + 1;
                            int ay = (o.seed * (k + 5) * 11) % (2 * R - 1) - R + 1;
                            if (ax * ax + ay * ay < R * R - 3) rect(c, ix + sway + ax, cy + ay, 1, 1, shade(o.col, 0.78f));
                        }
                        break;
                    }
                    case T_BUSH: {
                        for (int r = 0; r < o.h; r++) {
                            float f = (r + 1) / (float) o.h;
                            int rw = (int) (o.w * (0.5f + 0.5f * f));
                            rect(c, ix + (o.w - rw) / 2, iy - o.h + r, rw, 1, r < 2 ? shade(o.col, 1.15f) : o.col);
                        }
                        if (o.col2 != 0)
                            for (int k = 0; k < 3; k++)
                                rect(c, ix + 2 + (k * 5 + o.seed % 3) % (o.w - 3), iy - 1 - (k % Math.max(1, o.h - 2)), 1, 1, o.col2);
                        break;
                    }
                    case T_FLOWER: {
                        int sw = (int) Math.round(Math.sin(t / 700.0 + o.seed) * 0.7);
                        rect(c, ix + 1, iy - 3, 1, 3, Color.rgb(70, 150, 70));
                        rect(c, ix + sw, iy - 5, 3, 3, o.col);
                        rect(c, ix + 1 + sw, iy - 4, 1, 1, Color.rgb(255, 200, 60));
                        break;
                    }
                    case T_LAMP: {
                        rect(c, ix, iy - 12, 1, 12, Color.rgb(70, 70, 80));
                        if (theme != 0) {
                            int ga = 40 + (int) (10 * Math.sin(t / 200.0 + o.seed));
                            p.setColor(Color.argb(ga, 255, 225, 130));
                            c.drawRect(ix - 7, iy - 22, ix + 8, iy - 7, p);
                        }
                        rect(c, ix - 1, iy - 15, 3, 3, theme != 0 ? Color.rgb(255, 230, 130) : Color.rgb(200, 200, 170));
                        break;
                    }
                    case T_FENCE: {
                        int fc = theme == 2 ? Color.rgb(170, 165, 150) : Color.rgb(245, 238, 220);
                        for (int i = 0; i < o.w; i += 2) rect(c, ix + i, iy - 6, 1, 6, fc);
                        rect(c, ix, iy - 5, o.w, 1, shade(fc, 0.85f));
                        rect(c, ix, iy - 2, o.w, 1, shade(fc, 0.85f));
                        break;
                    }
                    case T_CAT:
                        drawCat(c, o.x, o.y - 10, 1f, o.look, t, o.seed);
                        break;
                }
            }

            for (float[] f : flyers) {
                float fx = f[0] + (float) Math.sin(t * 0.0009 * f[3] + f[2]) * 9;
                float fy = f[1] + (float) Math.cos(t * 0.0012 * f[3] + f[2] * 2) * 5;
                int id = (int) (f[2] * 10);
                if (theme == 2) {
                    if (((t / 300) + id) % 4 != 0) {
                        p.setColor(Color.argb(60, 255, 240, 120));
                        c.drawRect(fx - 2, fy - 2, fx + 3, fy + 3, p);
                        rect(c, fx, fy, 1, 1, Color.rgb(255, 245, 150));
                    }
                } else {
                    int wc = BF[id % BF.length];
                    boolean up = (t / 130 + id) % 2 == 0;
                    rect(c, fx, fy, 1, 1, Color.rgb(90, 60, 60));
                    rect(c, fx - 1, fy - (up ? 1 : 0), 1, up ? 2 : 1, wc);
                    rect(c, fx + 1, fy - (up ? 1 : 0), 1, up ? 2 : 1, wc);
                }
            }
        }

        // ---------- interface ----------
        void text(Canvas c, String s, float x, float y, float size, int col, Paint.Align al) {
            tp.setStyle(Paint.Style.FILL);
            tp.setTextSize(size);
            tp.setTextAlign(al);
            tp.setColor(Color.argb(140, 20, 10, 10));
            c.drawText(s, x + size * 0.05f, y + size * 0.06f, tp);
            tp.setColor(col);
            c.drawText(s, x, y, tp);
        }

        void btn(Canvas c, int id, float l, float t, float r, float b, String s, boolean on) {
            RectF rf = new RectF(l, t, r, b);
            hits.add(rf);
            hitIds.add(id);
            float rad = getWidth() * 0.03f;
            tp.setStyle(Paint.Style.FILL);
            tp.setColor(on ? CREAM : Color.argb(185, 70, 48, 40));
            c.drawRoundRect(rf, rad, rad, tp);
            float size = (b - t) * 0.42f;
            tp.setTextSize(size);
            tp.setTextAlign(Paint.Align.CENTER);
            tp.setColor(on ? BROWN : CREAM);
            c.drawText(s, (l + r) / 2, (t + b) / 2 + size * 0.35f, tp);
        }

        void swatch(Canvas c, int id, float l, float t, float sz, int col, boolean on) {
            RectF rf = new RectF(l, t, l + sz, t + sz);
            hits.add(rf);
            hitIds.add(id);
            float rad = getWidth() * 0.02f;
            tp.setStyle(Paint.Style.FILL);
            tp.setColor(col);
            c.drawRoundRect(rf, rad, rad, tp);
            if (on) {
                tp.setStyle(Paint.Style.STROKE);
                tp.setStrokeWidth(getWidth() * 0.01f);
                tp.setColor(Color.WHITE);
                c.drawRoundRect(l - 4, t - 4, l + sz + 4, t + sz + 4, rad, rad, tp);
                tp.setStyle(Paint.Style.FILL);
            }
        }

        String fmt(long ms) {
            long dt = ms / 100;
            return (dt / 10) + "," + (dt % 10) + " s";
        }

        void drawMenu(Canvas c, long t) {
            float w = getWidth(), h = getHeight();
            c.drawColor(Color.argb(95, 0, 0, 0));
            float y1 = topInset + 0.12f * h;
            text(c, "Où est", w / 2, y1 + 0.10f * w, w * 0.12f, CREAM, Paint.Align.CENTER);
            text(c, "mon chat ?", w / 2, y1 + 0.24f * w, w * 0.12f, CREAM, Paint.Align.CENTER);
            float k = w * 0.02f;
            drawCat(c, (w - 10 * k) / 2, y1 + 0.32f * w + 2 * k, k, look, t, 0);
            String[] names = {"Facile", "Moyen", "Difficile"};
            for (int i = 0; i < 3; i++) {
                float y = y1 + 0.66f * w + i * 0.16f * w;
                btn(c, 1 + i, 0.1f * w, y, 0.9f * w, y + 0.13f * w, names[i], true);
            }
            float y = y1 + 1.14f * w;
            btn(c, 4, 0.1f * w, y, 0.9f * w, y + 0.13f * w, "Personnaliser mon chat", true);
        }

        void drawCustom(Canvas c, long t) {
            float w = getWidth(), top = topInset;
            c.drawColor(Color.argb(150, 0, 0, 0));
            text(c, "Mon chat", w / 2, top + 0.11f * w, w * 0.09f, CREAM, Paint.Align.CENTER);
            float k = w * 0.028f;
            drawCat(c, (w - 10 * k) / 2, top + 0.16f * w + 2 * k, k, look, t, 0);
            float y0 = top + 0.58f * w, rowH = 0.21f * w, sz = 0.11f * w, gap = 0.025f * w;
            float lbl = w * 0.04f, lx = w * 0.07f;

            text(c, "Pelage", lx, y0, lbl, CREAM, Paint.Align.LEFT);
            float sx = (w - (6 * sz + 5 * gap)) / 2;
            for (int i = 0; i < 6; i++)
                swatch(c, 10 + i, sx + i * (sz + gap), y0 + 0.02f * w, sz, FUR[i], look[0] == i);

            text(c, "Yeux", lx, y0 + rowH, lbl, CREAM, Paint.Align.LEFT);
            sx = (w - (4 * sz + 3 * gap)) / 2;
            for (int i = 0; i < 4; i++)
                swatch(c, 20 + i, sx + i * (sz + gap), y0 + rowH + 0.02f * w, sz, EYE[i], look[1] == i);

            float pw = (0.88f * w - 3 * gap) / 4, ph = 0.1f * w, px0 = 0.06f * w;
            text(c, "Motif", lx, y0 + 2 * rowH, lbl, CREAM, Paint.Align.LEFT);
            for (int i = 0; i < 4; i++) {
                float l = px0 + i * (pw + gap), tt = y0 + 2 * rowH + 0.02f * w;
                btn(c, 30 + i, l, tt, l + pw, tt + ph, PAT[i], look[2] == i);
            }
            text(c, "Accessoire", lx, y0 + 3 * rowH, lbl, CREAM, Paint.Align.LEFT);
            for (int i = 0; i < 4; i++) {
                float l = px0 + i * (pw + gap), tt = y0 + 3 * rowH + 0.02f * w;
                btn(c, 40 + i, l, tt, l + pw, tt + ph, ACC[i], look[3] == i);
            }
            btn(c, 5, 0.2f * w, y0 + 4 * rowH, 0.8f * w, y0 + 4 * rowH + 0.12f * w, "Terminé", true);
        }

        void drawHud(Canvas c, long t) {
            float w = getWidth(), top = topInset;
            tp.setStyle(Paint.Style.FILL);
            tp.setColor(Color.argb(170, 40, 28, 24));
            c.drawRect(0, 0, w, top + w * 0.16f, tp);
            btn(c, 5, w * 0.03f, top + w * 0.02f, w * 0.15f, top + w * 0.14f, "<", true);
            text(c, ((t - startT) / 1000) + " s", w * 0.19f, top + w * 0.10f, w * 0.06f, CREAM, Paint.Align.LEFT);
            btn(c, 6, w * 0.40f, top + w * 0.025f, w * 0.60f, top + w * 0.135f, "Indice", true);
            text(c, "Cherche", w * 0.62f, top + w * 0.095f, w * 0.03f, CREAM, Paint.Align.LEFT);
            drawCat(c, w * 0.80f, top + w * 0.03f, w * 0.0112f, look, t, 0);

            if (t < hintUntil) {
                float a = (hintUntil - t) / 3500f;
                float rr = w * 0.2f * (1 + 0.06f * (float) Math.sin(t / 150.0));
                tp.setStyle(Paint.Style.STROKE);
                tp.setStrokeWidth(w * 0.012f);
                tp.setColor(Color.argb((int) (230 * Math.min(1f, a * 3)), 255, 255, 255));
                c.drawCircle(hintX, hintY, rr, tp);
                tp.setStyle(Paint.Style.FILL);
            }
            for (int i = marks.size() - 1; i >= 0; i--) {
                float[] m = marks.get(i);
                float a = (t - m[2]) / 700f;
                if (a >= 1) { marks.remove(i); continue; }
                tp.setStyle(Paint.Style.STROKE);
                tp.setStrokeWidth(w * 0.01f);
                tp.setColor(Color.argb((int) (220 * (1 - a)), 255, 240, 230));
                c.drawCircle(m[0], m[1], w * 0.02f + a * w * 0.07f, tp);
            }
            tp.setStyle(Paint.Style.FILL);
        }

        void drawWin(Canvas c) {
            float w = getWidth(), h = getHeight();
            float py = (target.y * ps < h * 0.5f) ? h * 0.5f : h * 0.08f + topInset;
            tp.setStyle(Paint.Style.FILL);
            tp.setColor(Color.argb(238, 255, 243, 224));
            c.drawRoundRect(w * 0.07f, py, w * 0.93f, py + 0.78f * w, w * 0.04f, w * 0.04f, tp);
            text(c, "Miaou ! Trouvé !", w / 2, py + 0.13f * w, w * 0.07f, BROWN, Paint.Align.CENTER);
            text(c, "Temps : " + fmt(foundT - startT), w / 2, py + 0.25f * w, w * 0.05f, BROWN, Paint.Align.CENTER);
            text(c, newBest ? "Nouveau record !" : "Record : " + fmt(bestMs), w / 2, py + 0.33f * w,
                    w * 0.04f, BROWN, Paint.Align.CENTER);
            btn(c, 7, 0.14f * w, py + 0.40f * w, 0.86f * w, py + 0.54f * w, "Rejouer", false);
            btn(c, 8, 0.14f * w, py + 0.58f * w, 0.86f * w, py + 0.72f * w, "Menu", false);
        }

        @Override
        protected void onDraw(Canvas c) {
            if (ps == 0) return;
            hits.clear();
            hitIds.clear();
            long t = SystemClock.uptimeMillis();
            c.save();
            c.scale(ps, ps);
            drawScene(c, t);
            if (state == WIN && target != null) {
                float jump = Math.abs((float) Math.sin(t / 220.0)) * 3f;
                drawCat(c, target.x, target.y - 10 - jump, 1f, target.look, t, target.seed);
                for (int k = 0; k < 8; k++) {
                    double a = k * 0.785 + t / 450.0;
                    float r = 9 + 3 * (float) Math.sin(t / 200.0 + k);
                    float sx = target.x + 5 + (float) Math.cos(a) * r;
                    float sy = target.y - 5 + (float) Math.sin(a) * r;
                    p.setColor(Color.rgb(255, 215, 90));
                    c.drawRect(sx, sy - 1, sx + 1, sy + 2, p);
                    c.drawRect(sx - 1, sy, sx + 2, sy + 1, p);
                }
            }
            c.restore();
            switch (state) {
                case MENU: drawMenu(c, t); break;
                case CUSTOM: drawCustom(c, t); break;
                case PLAY: drawHud(c, t); break;
                default: drawWin(c); break;
            }
        }

        // ---------- interactions ----------
        void onButton(int id) {
            if (id >= 1 && id <= 3) startGame(id - 1);
            else if (id == 4) state = CUSTOM;
            else if (id == 5) { if (state == PLAY) showMenu(); else state = MENU; }
            else if (id == 6 && target != null) {
                float w = getWidth();
                hintUntil = SystemClock.uptimeMillis() + 3500;
                hintX = (target.x + 5) * ps + (rnd.nextFloat() - 0.5f) * w * 0.3f;
                hintY = (target.y - 5) * ps + (rnd.nextFloat() - 0.5f) * w * 0.3f;
            }
            else if (id == 7) startGame(diff);
            else if (id == 8) showMenu();
            else if (id >= 10 && id < 20) { look[0] = id - 10; saveLook(); }
            else if (id >= 20 && id < 30) { look[1] = id - 20; saveLook(); }
            else if (id >= 30 && id < 40) { look[2] = id - 30; saveLook(); }
            else if (id >= 40 && id < 50) { look[3] = id - 40; saveLook(); }
        }

        void tapScene(float x, float y) {
            float w = getWidth();
            if (y < topInset + w * 0.16f) return;
            float lx = x / ps, ly = y / ps, tol = w * 0.04f / ps;
            if (lx >= target.x - tol && lx <= target.x + 10 + tol
                    && ly >= target.y - 10 - tol && ly <= target.y + tol) {
                foundT = SystemClock.uptimeMillis();
                long el = foundT - startT;
                String key = "best" + diff;
                long best = prefs.getLong(key, 0);
                newBest = best == 0 || el < best;
                if (newBest) { prefs.edit().putLong(key, el).apply(); best = el; }
                bestMs = best;
                state = WIN;
            } else {
                marks.add(new float[]{x, y, SystemClock.uptimeMillis()});
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() != MotionEvent.ACTION_UP) return true;
            float x = e.getX(), y = e.getY();
            for (int i = hits.size() - 1; i >= 0; i--) {
                if (hits.get(i).contains(x, y)) {
                    onButton(hitIds.get(i));
                    invalidate();
                    return true;
                }
            }
            if (state == PLAY && target != null) tapScene(x, y);
            invalidate();
            return true;
        }
    }
}
