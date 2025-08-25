package org.scalablytyped.converter.internal
package ts

trait MemberCache {
  def members: IArray[TsContainerOrDecl]

  lazy val (
    nameds: IArray[TsNamedDecl],
    exports: IArray[TsExport],
    imports: IArray[TsImport],
    unnamed: IArray[TsContainerOrDecl]
  ) =
    members.partitionCollect3(
      { case m: TsNamedDecl => m },
      { case x: TsExport => x },
      { case x: TsImport => x }
    )

  lazy val isModule: Boolean =
    exports.nonEmpty || imports.exists {
      case TsImport(_, _, _: TsImportee.Local) => false
      case _                                   => true
    }
}

trait HasClassMembers {
  def members: IArray[TsMember]

  lazy val (
    membersByName: Map[TsIdent, IArray[TsMember]],
    unnamed: IArray[TsMember]
  ) = {
    val (named, unnamed: IArray[TsMember]) =
      members.partitionCollect {
        case x: TsMemberCall     => x
        case x: TsMemberFunction => x
        case x: TsMemberProperty => x
        case x: TsMemberCtor     => x
      }

    val map = named.groupBy {
      case x: TsMemberFunction => x.name
      case x: TsMemberProperty => x.name
      case _: TsMemberCall     => TsIdent("apply") // Simplified for now
      case _: TsMemberCtor     => TsIdent("constructor") // Simplified for now
    }
    (map, unnamed)
  }
}
