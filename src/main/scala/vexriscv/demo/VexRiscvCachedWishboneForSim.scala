package vexriscv.demo

import spinal.core._
import spinal.lib._
import spinal.lib.bus.avalon.AvalonMM
import spinal.lib.eda.altera.{InterruptReceiverTag, QSysify, ResetEmitterTag}
import vexriscv.ip.{DataCacheConfig, InstructionCacheConfig}
import vexriscv.plugin._
import vexriscv.{VexRiscv, VexRiscvConfig, plugin}

/**
 * Created by spinalvm on 14.07.17.
 */
//class VexRiscvAvalon(debugClockDomain : ClockDomain) extends Component{
//
//}


// make clean run DBUS=CACHED_WISHBONE IBUS=CACHED_WISHBONE MMU=no CSR=no DEBUG_PLUGIN=no
object VexRiscvCachedWishboneForSim{
  def main(args: Array[String]) {
    val report = SpinalVerilog{

      //CPU configuration
      val cpuConfig = VexRiscvConfig(
        plugins = List(
          new PcManagerSimplePlugin(0x00000000l, false),
//          new IBusSimplePlugin(
//            interfaceKeepData = false,
//            catchAccessFault = false
//          ),
//          new DBusSimplePlugin(
//            catchAddressMisaligned = false,
//            catchAccessFault = false
//          ),
          new IBusCachedPlugin(
            config = InstructionCacheConfig(
              cacheSize = 4096,
              bytePerLine =32,
              wayCount = 1,
              addressWidth = 32,
              cpuDataWidth = 32,
              memDataWidth = 32,
              catchIllegalAccess = true,
              catchAccessFault = true,
              catchMemoryTranslationMiss = true,
              asyncTagMemory = false,
              twoCycleRam = true
            )
            //            askMemoryTranslation = true,
            //            memoryTranslatorPortConfig = MemoryTranslatorPortConfig(
            //              portTlbSize = 4
            //            )
          ),
          new DBusCachedPlugin(
            config = new DataCacheConfig(
              cacheSize         = 4096,
              bytePerLine       = 32,
              wayCount          = 1,
              addressWidth      = 32,
              cpuDataWidth      = 32,
              memDataWidth      = 32,
              catchAccessError  = true,
              catchIllegal      = true,
              catchUnaligned    = true,
              catchMemoryTranslationMiss = true
            ),
            memoryTranslatorPortConfig = null
            //            memoryTranslatorPortConfig = MemoryTranslatorPortConfig(
            //              portTlbSize = 6
            //            )
          ),
          new StaticMemoryTranslatorPlugin(
            ioRange      = _(31 downto 28) === 0xF
          ),
          new DecoderSimplePlugin(
            catchIllegalInstruction = true
          ),
          new RegFilePlugin(
            regFileReadyKind = plugin.SYNC,
            zeroBoot = false
          ),
          new IntAluPlugin,
          new SrcPlugin(
            separatedAddSub = false,
            executeInsertion = true
          ),
          new FullBarrielShifterPlugin,
          new MulPlugin,
          new DivPlugin,
          new HazardSimplePlugin(
            bypassExecute           = true,
            bypassMemory            = true,
            bypassWriteBack         = true,
            bypassWriteBackBuffer   = true,
            pessimisticUseSrc       = false,
            pessimisticWriteRegFile = false,
            pessimisticAddressMatch = false
          ),
//          new DebugPlugin(ClockDomain.current.clone(reset = Bool().setName("debugReset"))),
          new BranchPlugin(
            earlyBranch = false,
            catchAddressMisaligned = true,
            prediction = STATIC
          ),
          new CsrPlugin(
            config = CsrPluginConfig.small(mtvecInit = 0x80000020l)
          ),
          new YamlPlugin("cpu0.yaml")
        )
      )

      //CPU instanciation
      val cpu = new VexRiscv(cpuConfig)

      //CPU modifications to be an Avalon one
      //cpu.setDefinitionName("VexRiscvAvalon")
      cpu.rework {
        for (plugin <- cpuConfig.plugins) plugin match {
//          case plugin: IBusSimplePlugin => {
//            plugin.iBus.asDirectionLess() //Unset IO properties of iBus
//            iBus = master(plugin.iBus.toAvalon())
//              .setName("iBusAvalon")
//              .addTag(ClockDomainTag(ClockDomain.current)) //Specify a clock domain to the iBus (used by QSysify)
//          }
          case plugin: IBusCachedPlugin => {
            plugin.iBus.asDirectionLess() //Unset IO properties of iBus
            master(plugin.iBus.toWishbone()).setName("iBusWishbone")
          }
//          case plugin: DBusSimplePlugin => {
//            plugin.dBus.asDirectionLess()
//            master(plugin.dBus.toAvalon())
//              .setName("dBusAvalon")
//              .addTag(ClockDomainTag(ClockDomain.current))
//          }
          case plugin: DBusCachedPlugin => {
            plugin.dBus.asDirectionLess()
            master(plugin.dBus.toWishbone()).setName("dBusWishbone")
          }
//          case plugin: DebugPlugin => {
//            plugin.io.bus.asDirectionLess()
//            slave(plugin.io.bus.fromAvalon())
//              .setName("debugBusAvalon")
//              .addTag(ClockDomainTag(plugin.debugClockDomain))
//              .parent = null  //Avoid the io bundle to be interpreted as a QSys conduit
//            plugin.io.resetOut
//              .addTag(ResetEmitterTag(plugin.debugClockDomain))
//              .parent = null //Avoid the io bundle to be interpreted as a QSys conduit
//          }
          case _ =>
        }
      }
      cpu
    }

    //Generate the QSys TCL script to integrate the CPU
    QSysify(report.toplevel)
  }
}