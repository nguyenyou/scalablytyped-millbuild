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
