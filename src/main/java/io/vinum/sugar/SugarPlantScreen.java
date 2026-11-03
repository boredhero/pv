package io.vinum.sugar;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Inputs on the left, processing in the center, finished liquids and solids on the right. */
public final class SugarPlantScreen extends AbstractContainerScreen<SugarPlantMenu> {
    private final Button[] modes=new Button[5];
    private Button start,recycle,recover;
    private static final String[] MODES={"syrup","sugar","panela","mead","wash"};
    private static final String[] STATUS={"ready","running","wrong_input","output_full","solid_full","fuel","ingredient"};
    public SugarPlantScreen(SugarPlantMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);imageWidth=340;imageHeight=222;inventoryLabelX=89;inventoryLabelY=127;
    }
    private static Component text(String key){return Component.translatable("gui.vinum.sugar_plant."+key);}
    @Override protected void init() {
        super.init();
        for(int i=0;i<modes.length;i++) {
            int mode=i;
            modes[i]=addRenderableWidget(Button.builder(text(MODES[i]),b->send(mode))
                .bounds(leftPos+8+i*65,topPos+20,62,18)
                .tooltip(Tooltip.create(text(MODES[i]+"_hint"))).build());
        }
        start=addRenderableWidget(Button.builder(text("start"),b->send(100)).bounds(leftPos+82,topPos+90,74,18).build());
        recycle=addRenderableWidget(Button.builder(text("next_stage"),b->send(101)).bounds(leftPos+168,topPos+90,86,18)
            .tooltip(Tooltip.create(text("next_stage_hint"))).build());
        recover=addRenderableWidget(Button.builder(text("recover"),b->send(102)).bounds(leftPos+267,topPos+182,65,18)
            .tooltip(Tooltip.create(text("recover_hint"))).build());
        updateButtons();
    }
    private void send(int action){if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);}
    private void updateButtons() {
        for(int i=0;i<modes.length;i++)modes[i].active=menu.required()==0&&menu.mode()!=i;
        start.active=menu.status()==0;
        recycle.active=menu.required()==0&&menu.outputAmount()>0;
        recover.active=menu.required()==0&&menu.inputAmount()>0;
    }
    @Override protected void containerTick(){super.containerTick();updateButtons();}
    @Override protected void renderBg(GuiGraphics g,float partialTick,int mouseX,int mouseY) {
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFF332C29);
        g.fill(leftPos+2,topPos+2,leftPos+imageWidth-2,topPos+imageHeight-2,0xFF98714E);
        g.fill(leftPos+5,topPos+5,leftPos+imageWidth-5,topPos+imageHeight-5,0xFFE4D8C3);
        for(var slot:menu.slots) {
            int x=leftPos+slot.x,y=topPos+slot.y;
            g.fill(x-1,y-1,x+17,y+17,0xFF625449);g.fill(x,y,x+16,y+16,0xFFB8AA94);
        }
        tank(g,18,50,menu.inputAmount(),menu.inputColor());tank(g,304,50,menu.outputAmount(),menu.outputColor());
        g.fill(leftPos+82,topPos+78,leftPos+254,topPos+84,0xFF564D43);
        int progress=menu.required()<=0?0:Math.clamp(menu.progress()*170/menu.required(),0,170);
        g.fill(leftPos+83,topPos+79,leftPos+83+progress,topPos+83,0xFF71925A);
        int fire=menu.burnTotal()<=0?0:Math.clamp(menu.burn()*16/menu.burnTotal(),0,16);
        g.fill(leftPos+39,topPos+163-fire,leftPos+43,topPos+163,0xFFE48C2C);
        g.drawString(font,"→",leftPos+68,topPos+58,0xFF594736,false);
        g.drawString(font,"↓",leftPos+53,topPos+75,0xFF594736,false);
        g.drawString(font,"↓",leftPos+279,topPos+75,0xFF594736,false);
    }
    private void tank(GuiGraphics g,int x,int y,int amount,int color) {
        x+=leftPos;y+=topPos;
        g.fill(x-1,y-1,x+19,y+53,0xFF625449);g.fill(x,y,x+18,y+52,0xFF302D29);
        int height=Math.clamp(amount*52/SugarPlantBlockEntity.TANK_CAPACITY,0,52);
        g.fill(x,y+52-height,x+18,y+52,color|0xFF000000);
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY) {
        super.renderLabels(g,mouseX,mouseY);
        g.drawString(font,text("input"),10,40,0x493B2D,false);
        g.drawString(font,text("output"),302,40,0x493B2D,false);
        g.drawString(font,text("load"),44,42,0x493B2D,false);
        g.drawString(font,text("bottles"),264,42,0x493B2D,false);
        g.drawString(font,text("returned"),42,106,0x493B2D,false);
        g.drawString(font,text("filled"),268,106,0x493B2D,false);
        g.drawString(font,text("fuel_slot"),14,135,0x493B2D,false);
        g.drawString(font,text("ingredient_slot"),14,172,0x493B2D,false);
        g.drawString(font,text("solid_slot"),295,135,0x493B2D,false);
        int mode=Math.clamp(menu.mode(),0,4);
        g.drawWordWrap(font,text(MODES[mode]+"_recipe"),82,45,172,0x493B2D);
        int status=Math.clamp(menu.status(),0,STATUS.length-1);
        g.drawWordWrap(font,text("status_"+STATUS[status]),10,116,320,status==0?0x355125:0x715432);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
        super.render(g,mouseX,mouseY,partialTick);renderTooltip(g,mouseX,mouseY);
        if(isHovering(18,50,18,52,mouseX,mouseY)||isHovering(304,50,18,52,mouseX,mouseY)) {
            boolean input=isHovering(18,50,18,52,mouseX,mouseY);
            var fluid=input?menu.input():menu.output();int amount=input?menu.inputAmount():menu.outputAmount();
            g.renderComponentTooltip(font,java.util.List.of(fluid.isEmpty()?Component.translatable("gui.vinum.empty_tank"):fluid.getHoverName(),
                Component.translatable("tooltip.vinum.container_capacity",amount,SugarPlantBlockEntity.TANK_CAPACITY)),mouseX,mouseY);
        }
        String[] hints={"load_hint","returned_hint","bottle_hint","filled_hint","fuel_hint","ingredient_hint","solid_hint"};
        for(int i=0;i<7;i++) {
            var slot=menu.slots.get(i);
            if(!slot.hasItem()&&isHovering(slot.x,slot.y,16,16,mouseX,mouseY))
                g.renderTooltip(font,text(hints[i]),mouseX,mouseY);
        }
    }
}
