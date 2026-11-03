package io.vinum.blender;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Exact volume controls; preparation modes show their fixed ingredient recipe instead. */
public final class BlenderScreen extends AbstractContainerScreen<BlenderMenu>{
    private static final String[] MODES={"blend","honey","wash","abv"};
    private static final String[] STATUSES={"ready","running","input","output","return","ingredient","incompatible"};
    private final Button[] modes=new Button[4];
    private Button start,automatic,recoverA,recoverB;
    private RatioSlider slider;
    public BlenderScreen(BlenderMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title);imageWidth=340;imageHeight=234;inventoryLabelX=89;inventoryLabelY=141;
    }
    private static Component text(String key){return Component.translatable("gui.vinum.blender."+key);}
    private void send(int action){if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);}
    @Override protected void init(){
        super.init();
        for(int i=0;i<4;i++){
            int mode=i;modes[i]=addRenderableWidget(Button.builder(text(MODES[i]),b->send(mode))
                .bounds(leftPos+8+i*81,topPos+20,78,18).tooltip(Tooltip.create(text(MODES[i]+"_hint"))).build());
        }
        automatic=addRenderableWidget(Button.builder(text("automatic_off"),b->send(103)).bounds(leftPos+188,topPos+3,144,16)
            .tooltip(Tooltip.create(text("automatic_hint"))).build());
        slider=addRenderableWidget(new RatioSlider(leftPos+10,topPos+110));
        start=addRenderableWidget(Button.builder(text("start"),b->send(100)).bounds(leftPos+198,topPos+110,58,18).build());
        recoverA=addRenderableWidget(Button.builder(text("drain_a"),b->send(104)).bounds(leftPos+8,topPos+185,70,18).build());
        recoverB=addRenderableWidget(Button.builder(text("drain_b"),b->send(105)).bounds(leftPos+8,topPos+207,70,18).build());
        updateControls();
    }
    private void updateControls(){
        for(int i=0;i<4;i++)modes[i].active=menu.required()==0&&menu.mode()!=i;
        automatic.setMessage(text(menu.automatic()?"automatic_on":"automatic_off"));
        start.active=menu.status()==0;
        slider.visible=menu.mode()==0||menu.mode()==3;
        slider.active=menu.required()==0&&!menu.automatic();
        slider.sync();
        recoverA.active=menu.required()==0&&menu.aAmount()>0;
        recoverB.active=menu.required()==0&&menu.bAmount()>0;
    }
    @Override protected void containerTick(){super.containerTick();updateControls();}
    private final class RatioSlider extends AbstractSliderButton{
        private boolean dragging;
        RatioSlider(int x,int y){super(x,y,180,18,Component.empty(),menu.ratio()/100D);updateMessage();}
        void sync(){if(!dragging){value=menu.mode()==3?(menu.targetAbv()-1)/949D:menu.ratio()/100D;updateMessage();}}
        private int selected(){return menu.mode()==3?1+(int)Math.round(value*949):(int)Math.round(value*100);}
        @Override protected void updateMessage(){
            int n=selected();setMessage(menu.mode()==3?Component.translatable("gui.vinum.blender.target",String.format(java.util.Locale.ROOT,"%.1f",n/10D))
                :Component.translatable("gui.vinum.blender.ratio",n,100-n));
            setTooltip(Tooltip.create(text(menu.mode()==3?"abv_hint":"ratio_hint")));
        }
        @Override protected void applyValue(){send((menu.mode()==3?2000:1000)+selected());}
        @Override public boolean keyPressed(int key,int scan,int modifiers){
            if(active&&(key==262||key==263)){
                int n=Math.clamp(selected()+(key==262?1:-1),menu.mode()==3?1:0,menu.mode()==3?950:100);
                value=menu.mode()==3?(n-1)/949D:n/100D;updateMessage();applyValue();return true;
            }
            return super.keyPressed(key,scan,modifiers);
        }
        @Override public void onClick(double x,double y){dragging=true;super.onClick(x,y);}
        @Override public void onRelease(double x,double y){dragging=false;super.onRelease(x,y);}
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFF332C29);
        g.fill(leftPos+2,topPos+2,leftPos+imageWidth-2,topPos+imageHeight-2,0xFF89735B);
        g.fill(leftPos+5,topPos+5,leftPos+imageWidth-5,topPos+imageHeight-5,0xFFE4D8C3);
        for(var slot:menu.slots){int x=leftPos+slot.x,y=topPos+slot.y;g.fill(x-1,y-1,x+17,y+17,0xFF625449);g.fill(x,y,x+16,y+16,0xFFB8AA94);}
        tank(g,18,menu.aAmount(),menu.color(0));tank(g,110,menu.bAmount(),menu.color(1));tank(g,304,menu.outputAmount(),menu.color(2));
        g.fill(leftPos+174,topPos+94,leftPos+256,topPos+100,0xFF564D43);
        int progress=menu.required()==0?0:Math.clamp(menu.progress()*80/menu.required(),0,80);
        g.fill(leftPos+175,topPos+95,leftPos+175+progress,topPos+99,0xFF71925A);
    }
    private void tank(GuiGraphics g,int x,int amount,int color){
        x+=leftPos;int y=topPos+57;
        g.fill(x-1,y-1,x+19,y+43,0xFF625449);g.fill(x,y,x+18,y+42,0xFF302D29);
        int height=Math.clamp(amount*42/BlenderBlockEntity.TANK_CAPACITY,0,42);
        g.fill(x,y+42-height,x+18,y+42,color|0xFF000000);
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY){
        super.renderLabels(g,mouseX,mouseY);
        g.drawString(font,text("input_a"),10,45,0x493B2D,false);g.drawString(font,text("input_b"),102,45,0x493B2D,false);
        g.drawString(font,text("output_label"),298,45,0x493B2D,false);
        g.drawString(font,text("load"),44,45,0x493B2D,false);g.drawString(font,text("load"),136,45,0x493B2D,false);
        g.drawString(font,text("bottle"),264,45,0x493B2D,false);
        g.drawString(font,text("returned"),42,101,0x493B2D,false);g.drawString(font,text("returned"),134,101,0x493B2D,false);
        g.drawString(font,text("filled"),268,101,0x493B2D,false);
        g.drawString(font,text("ingredient"),14,146,0x493B2D,false);g.drawString(font,text("returned"),48,146,0x493B2D,false);
        if(menu.mode()==0)g.drawWordWrap(font,Component.translatable("gui.vinum.blender.batch",menu.ratio()*10,(100-menu.ratio())*10),174,46,86,0x493B2D);
        else g.drawWordWrap(font,text(MODES[Math.clamp(menu.mode(),0,3)]+"_summary"),174,46,86,0x493B2D);
        if(menu.mode()==1||menu.mode()==2)g.drawWordWrap(font,text(MODES[menu.mode()]+"_recipe"),10,110,180,0x493B2D);
        g.drawString(font,text("status_"+STATUSES[Math.clamp(menu.status(),0,6)]),10,131,menu.status()==0?0x355125:0x715432,false);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        super.render(g,mouseX,mouseY,partial);renderTooltip(g,mouseX,mouseY);
        int[] xs={18,110,304};int[] amounts={menu.aAmount(),menu.bAmount(),menu.outputAmount()};
        for(int i=0;i<3;i++)if(isHovering(xs[i],57,18,42,mouseX,mouseY)){
            var fluid=menu.fluid(i);var lines=new java.util.ArrayList<Component>();
            lines.add(fluid.isEmpty()?Component.translatable("gui.vinum.empty_tank"):fluid.getHoverName());
            lines.add(Component.translatable("tooltip.vinum.container_capacity",amounts[i],BlenderBlockEntity.TANK_CAPACITY));
            if(!fluid.isEmpty())lines.add(Component.literal(String.format(java.util.Locale.ROOT,"%.2f%% ABV",io.vinum.production.BatchProfile.of(fluid).alcoholFraction()*100)));
            g.renderComponentTooltip(font,lines,mouseX,mouseY);
        }
        String[] hints={"load_a_hint","return_hint","load_b_hint","return_hint","bottle_hint","filled_hint","ingredient_hint","return_hint"};
        for(int i=0;i<8;i++){var slot=menu.slots.get(i);if(!slot.hasItem()&&isHovering(slot.x,slot.y,16,16,mouseX,mouseY))g.renderTooltip(font,text(hints[i]),mouseX,mouseY);}
    }
}
